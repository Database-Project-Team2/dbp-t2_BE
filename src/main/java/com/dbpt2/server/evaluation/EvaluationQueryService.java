package com.dbpt2.server.evaluation;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.Csv;
import com.dbpt2.server.common.ErrorCode;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.evaluation.EvaluationDtos.EligibleProject;
import com.dbpt2.server.evaluation.EvaluationDtos.EvaluationOptions;
import com.dbpt2.server.evaluation.EvaluationDtos.EvaluationResult;
import com.dbpt2.server.evaluation.EvaluationDtos.Evaluator;
import com.dbpt2.server.evaluation.EvaluationDtos.Person;
import com.dbpt2.server.member.ProjectMember;
import com.dbpt2.server.member.ProjectMemberRepository;
import com.dbpt2.server.member.ProjectRole;
import com.dbpt2.server.project.Project;
import com.dbpt2.server.project.ProjectRepository;
import com.dbpt2.server.security.AuthUser;
import com.dbpt2.server.status.StatusRules;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 평가 조회. 행 범위: 경영진 전체 / 직원 본인 소속 프로젝트. 의견: 경영진과 해당 프로젝트 PM만(CSV도 동일). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationQueryService {

    private static final Map<EvaluationType, String> TYPE_LABELS =
            Map.of(EvaluationType.CLIENT, "고객평가", EvaluationType.PM, "PM 평가", EvaluationType.PEER, "동료평가");

    private final EvaluationSearch evaluationSearch;
    private final EvaluationRepository evaluationRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public record Filter(String targetEmployeeName, Integer targetEmployeeId, Integer projectId, EvaluationType type,
                         LocalDate from, LocalDate to) {}

    public Page<EvaluationResult> search(AuthUser user, Filter f, Pageable pageable) {
        Set<Integer> commentProjects = commentProjects(user);
        return evaluationSearch.search(criteria(user, f), pageable).map(ev -> toResult(ev, commentProjects));
    }

    public EvaluationResult detail(AuthUser user, int evaluationId) {
        Evaluation ev = evaluationRepository.findById(evaluationId).orElseThrow(ApiException::notFound);
        int projectId = ev.getTarget().getProject().getId();
        accessPolicy.requireProjectVisible(user, projectId);
        return toResult(ev, commentProjects(user));
    }

    /** 목록과 같은 행·의견 권한. 의견을 볼 수 있는 행이 하나라도 있으면 의견 열을 두고, 권한 없는 행은 비운다. */
    public String csv(AuthUser user, Filter f) {
        Set<Integer> commentProjects = commentProjects(user);
        List<EvaluationResult> rows = evaluationSearch.searchAll(criteria(user, f)).stream().map(ev -> toResult(ev, commentProjects)).toList();
        boolean withComments = rows.stream().anyMatch(EvaluationResult::commentsVisible);
        List<String> headers = new ArrayList<>(List.of("프로젝트", "피평가자", "사번", "평가유형", "실제 평가자", "등록자", "평가일",
                "업무수행 점수", "커뮤니케이션 점수"));
        if (withComments) {
            headers.addAll(List.of("업무수행 의견", "커뮤니케이션 의견"));
        }
        List<List<Object>> lines = new ArrayList<>();
        for (EvaluationResult r : rows) {
            List<Object> line = new ArrayList<>(List.of(r.projectName(), r.target().employeeName(), r.target().employeeNo(),
                    TYPE_LABELS.get(r.evaluationType()), r.evaluator().name(), r.registeredBy().employeeName(), r.evaluatedDate(),
                    r.workPerformanceScore(), r.communicationScore()));
            if (withComments) {
                line.add(r.commentsVisible() ? r.workPerformanceComment() : "");
                line.add(r.commentsVisible() ? r.communicationComment() : "");
            }
            lines.add(line);
        }
        return Csv.content(headers, lines);
    }

    /** 평가할 수 있는 프로젝트: 본인이 실제 참여한 종료 프로젝트와 허용 유형·남은 대상 수. */
    public List<EligibleProject> eligibleProjects(AuthUser user) {
        List<ProjectMember> mine = memberRepository.findByEmployee(user.employeeId()).stream()
                .filter(m -> m.getProject().getActualEndDate() != null && StatusRules.isActualParticipant(m)).toList();
        if (mine.isEmpty()) {
            return List.of();
        }
        List<Integer> projectIds = mine.stream().map(m -> m.getProject().getId()).toList();
        List<Evaluation> evaluations = evaluationRepository.findByProjects(projectIds);
        List<EligibleProject> result = new ArrayList<>();
        for (ProjectMember own : mine) {
            Project p = own.getProject();
            List<ProjectMember> team = memberRepository.findTeam(p.getId());
            List<Evaluation> projectEvaluations = evaluations.stream().filter(ev -> ev.getTarget().getProject().getId().equals(p.getId())).toList();
            List<EvaluationType> allowed = new ArrayList<>();
            long remaining = 0;
            for (EvaluationType type : List.of(EvaluationType.PEER, EvaluationType.PM, EvaluationType.CLIENT)) {
                if (type != EvaluationType.PEER && own.getRole() != ProjectRole.PM) {
                    continue;
                }
                allowed.add(type);
                remaining += EvaluationRules.availableTargets(p, team, projectEvaluations, user.employeeId(), type).size();
            }
            long submitted = projectEvaluations.stream().filter(ev -> ev.getRegisteredBy().getId().equals(own.getId())).count();
            result.add(new EligibleProject(p.getId(), p.getName(), p.getActualEndDate(), own.getId(), own.getRole(), allowed,
                    remaining, submitted, StatusRules.workflowStatus(p)));
        }
        return result;
    }

    /** 평가 작성 화면용: 유형별 대상·평가자·제출 가능 여부. 구성원이 아니면 404, 실제 참여자·역할 조건이 안 맞으면 403. */
    public EvaluationOptions options(AuthUser user, int projectId, EvaluationType type) {
        if (!accessPolicy.isMemberOf(user, projectId)) {
            throw ApiException.notFound();
        }
        Project p = projectRepository.findWithClient(projectId).orElseThrow(ApiException::notFound);
        List<ProjectMember> team = memberRepository.findTeam(projectId);
        ProjectMember own = EvaluationRules.ownParticipant(team, user.employeeId())
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "실제 투입 이력이 있는 구성원만 평가할 수 있어요."));
        if (type != EvaluationType.PEER && own.getRole() != ProjectRole.PM) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이 평가유형은 해당 프로젝트의 PM만 작성할 수 있어요.");
        }
        String blocked = EvaluationRules.blockedReason(p, team, user.employeeId(), type);
        List<Person> targets = EvaluationRules.availableTargets(p, team, evaluationRepository.findByProjects(List.of(projectId)),
                user.employeeId(), type).stream().map(Person::of).toList();
        if (blocked == null && targets.isEmpty()) {
            blocked = "이 유형에서 작성할 수 있는 평가를 모두 제출했어요.";
        }
        Evaluator evaluator = type == EvaluationType.CLIENT
                ? new Evaluator(p.getClient().getContactName(), null, true, p.getClient().getName())
                : new Evaluator(own.getEmployee().getName(), own.getEmployee().getId(), false, null);
        LocalDate min = p.getActualEndDate();
        if (type == EvaluationType.CLIENT && p.getEvaluationRequestedDate() != null && min != null
                && p.getEvaluationRequestedDate().isAfter(min)) {
            min = p.getEvaluationRequestedDate();
        }
        return new EvaluationOptions(p.getId(), p.getName(), type, blocked == null, blocked, evaluator, Person.of(own), targets,
                min, TimeConfig.today(clock));
    }

    private EvaluationSearch.Criteria criteria(AuthUser user, Filter f) {
        if (f.from() != null && f.to() != null && f.from().isAfter(f.to())) {
            throw ApiException.invalid("to", "기간의 끝은 시작 이후여야 해요.");
        }
        return new EvaluationSearch.Criteria(f.targetEmployeeName(), f.targetEmployeeId(), f.projectId(), f.type(), f.from(), f.to(),
                accessPolicy.isManagement(user) ? null : user.employeeId());
    }

    /** 의견을 볼 수 있는 프로젝트. 경영진은 전체(null). */
    private Set<Integer> commentProjects(AuthUser user) {
        return accessPolicy.isManagement(user) ? null : new HashSet<>(memberRepository.findPmProjectIds(user.employeeId()));
    }

    private static EvaluationResult toResult(Evaluation ev, Set<Integer> commentProjects) {
        ProjectMember target = ev.getTarget();
        Project p = target.getProject();
        boolean comments = commentProjects == null || commentProjects.contains(p.getId());
        Evaluator evaluator = ev.getType() == EvaluationType.CLIENT
                ? new Evaluator(p.getClient().getContactName(), null, true, p.getClient().getName())
                : new Evaluator(ev.getEvaluator().getEmployee().getName(), ev.getEvaluator().getEmployee().getId(), false, null);
        return new EvaluationResult(ev.getId(), p.getId(), p.getName(), target.getId(), Person.of(target), ev.getType(),
                ev.getEvaluator() == null ? null : ev.getEvaluator().getId(), evaluator, ev.getRegisteredBy().getId(),
                Person.of(ev.getRegisteredBy()), ev.getEvaluatedDate(), ev.getWorkPerformanceScore(),
                comments ? ev.getWorkPerformanceComment() : null, ev.getCommunicationScore(),
                comments ? ev.getCommunicationComment() : null, comments);
    }
}
