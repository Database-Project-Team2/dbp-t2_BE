package com.dbpt2.server.evaluation;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.ErrorCode;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.evaluation.EvaluationDtos.CreateEvaluationRequest;
import com.dbpt2.server.evaluation.EvaluationDtos.CreatedEvaluation;
import com.dbpt2.server.evaluation.EvaluationDtos.WorkflowResponse;
import com.dbpt2.server.evaluation.EvaluationDtos.WorkflowStep;
import com.dbpt2.server.member.ProjectMember;
import com.dbpt2.server.member.ProjectMemberRepository;
import com.dbpt2.server.member.ProjectRole;
import com.dbpt2.server.project.Project;
import com.dbpt2.server.project.ProjectRepository;
import com.dbpt2.server.project.ProjectStats;
import com.dbpt2.server.security.AuthUser;
import com.dbpt2.server.status.StatusRules;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 평가 등록과 고객평가 단계 기록(명세 5장). 평가자·등록자는 토큰의 직원으로 서버가 정한다.
 * 고객평가 대상당 1건은 UNIQUE로 막히지 않으므로(evaluator NULL) 프로젝트 행을 먼저 잠그고 검사한다.
 */
@Service
@RequiredArgsConstructor
public class EvaluationCommandService {

    static final int COMMENT_MAX = 2000;

    private final EvaluationRepository evaluationRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectStats stats;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    /*
     * 같은 프로젝트의 평가 등록·고객평가 단계 기록은 프로젝트 행 잠금으로 직렬화한다.
     * 대상 구성원 행을 잠그면 서로를 평가하는 두 요청이 FK 공유 잠금과 엇갈려 교착 상태가 된다.
     * READ_COMMITTED: 잠금을 기다린 뒤의 중복 검사가 먼저 커밋된 평가를 보도록 한다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CreatedEvaluation create(AuthUser user, CreateEvaluationRequest r) {
        int projectId = memberRepository.findProjectId(r.targetMemberId())
                .orElseThrow(() -> ApiException.field(ErrorCode.NOT_FOUND, "target_member_id", "평가 대상을 찾을 수 없어요."));
        Project project = projectRepository.findByIdForUpdate(projectId).orElseThrow(ApiException::notFound);
        ProjectMember target = memberRepository.findById(r.targetMemberId())
                .orElseThrow(() -> ApiException.field(ErrorCode.NOT_FOUND, "target_member_id", "평가 대상을 찾을 수 없어요."));
        if (!accessPolicy.isMemberOf(user, project.getId())) {
            throw ApiException.field(ErrorCode.NOT_FOUND, "target_member_id", "평가 대상을 찾을 수 없어요.");
        }
        validateContent(r);
        List<ProjectMember> team = memberRepository.findTeam(project.getId());
        EvaluationType type = r.evaluationType();
        if (project.getActualEndDate() == null) {
            throw ApiException.rule("프로젝트가 종료된 후 평가할 수 있어요.");
        }
        ProjectMember own = EvaluationRules.ownParticipant(team, user.employeeId())
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "실제 투입 이력이 있는 구성원만 평가할 수 있어요."));
        if (type != EvaluationType.PEER && own.getRole() != ProjectRole.PM) {
            throw new ApiException(ErrorCode.FORBIDDEN, type == EvaluationType.CLIENT
                    ? "고객평가는 해당 프로젝트의 PM만 등록할 수 있어요." : "PM 평가는 해당 프로젝트의 PM만 작성할 수 있어요.");
        }
        ProjectMember lockedTarget = team.stream().filter(m -> m.getId().equals(target.getId())).findFirst().orElseThrow();
        if (!StatusRules.isActualParticipant(lockedTarget)) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "target_member_id", "실제 투입 이력이 있는 구성원만 평가 대상이에요.");
        }
        if (type != EvaluationType.CLIENT && lockedTarget.getId().equals(own.getId())) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "target_member_id", "자기 자신을 평가할 수 없어요.");
        }
        if (type == EvaluationType.PM && lockedTarget.getRole() == ProjectRole.PM) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "target_member_id", "PM 평가는 PM을 제외한 팀원에게 작성해요.");
        }
        LocalDate date = r.evaluatedDate();
        if (date.isAfter(today())) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "evaluated_date", "평가일은 오늘 이후일 수 없어요.");
        }
        if (date.isBefore(project.getActualEndDate())) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "evaluated_date", "평가일은 프로젝트 실제 종료일 이후여야 해요.");
        }
        if (type == EvaluationType.CLIENT) {
            if (project.getEvaluationRequestedDate() == null) {
                throw new ApiException(ErrorCode.WORKFLOW_CONFLICT, "고객평가 의뢰일을 먼저 기록해 주세요.");
            }
            if (project.getEvaluationReportedDate() != null) {
                throw new ApiException(ErrorCode.WORKFLOW_CONFLICT, "보고를 완료한 고객평가는 추가할 수 없어요.");
            }
            if (date.isBefore(project.getEvaluationRequestedDate())) {
                throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "evaluated_date", "고객평가일은 의뢰일 이후여야 해요.");
            }
            if (evaluationRepository.existsByTargetIdAndType(target.getId(), EvaluationType.CLIENT)) {
                throw new ApiException(ErrorCode.EVALUATION_DUPLICATED, "이 구성원의 고객평가는 이미 등록됐어요. 대상당 한 번만 등록할 수 있어요.");
            }
        } else if (evaluationRepository.existsByTargetIdAndTypeAndEvaluatorId(target.getId(), type, own.getId())) {
            throw new ApiException(ErrorCode.EVALUATION_DUPLICATED, "이미 제출한 평가가 있어요. 같은 대상·유형·평가자의 평가는 한 번만 등록할 수 있어요.");
        }
        try {
            Evaluation saved = evaluationRepository.saveAndFlush(Evaluation.create(target, type,
                    type == EvaluationType.CLIENT ? null : own, own, date,
                    r.workPerformanceScore(), r.workPerformanceComment().trim(),
                    r.communicationScore(), r.communicationComment().trim()));
            return new CreatedEvaluation(saved.getId(), project.getId(), target.getId(), type, date);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("uq_internal_evaluation")) {
                throw new ApiException(ErrorCode.EVALUATION_DUPLICATED);
            }
            throw e;
        }
    }

    /** 고객평가 단계: REQUEST → RECEIVE → REPORT. 해당 프로젝트 PM만. 프로젝트 행을 잠그고 검사한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WorkflowResponse recordWorkflow(AuthUser user, int projectId, WorkflowStep step, LocalDate date) {
        Project project = projectRepository.findByIdForUpdate(projectId).orElseThrow(ApiException::notFound);
        accessPolicy.requireProjectVisible(user, projectId);
        if (!accessPolicy.isPmOf(user, projectId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "고객평가 진행 상태는 해당 프로젝트 PM만 기록할 수 있어요.");
        }
        if (project.getActualEndDate() == null) {
            throw ApiException.rule("프로젝트 종료 후 고객평가를 의뢰할 수 있어요.");
        }
        if (date.isAfter(today())) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "processed_date", "처리일은 오늘 이후일 수 없어요.");
        }
        if (date.isBefore(project.getActualEndDate())) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "processed_date", "처리일은 프로젝트 종료일 이후여야 해요.");
        }
        WorkflowStep expected = project.getEvaluationRequestedDate() == null ? WorkflowStep.REQUEST
                : project.getEvaluationReceivedDate() == null ? WorkflowStep.RECEIVE
                : project.getEvaluationReportedDate() == null ? WorkflowStep.REPORT : null;
        if (expected == null) {
            throw new ApiException(ErrorCode.WORKFLOW_CONFLICT, "이미 보고가 완료됐어요.");
        }
        if (step != expected) {
            throw new ApiException(ErrorCode.WORKFLOW_CONFLICT, "지금은 " + expected + " 단계만 기록할 수 있어요.");
        }
        switch (step) {
            case REQUEST -> project.requestEvaluation(date);
            case RECEIVE -> receive(project, date);
            case REPORT -> {
                if (date.isBefore(project.getEvaluationReceivedDate())) {
                    throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "processed_date", "보고일은 회수일 이후여야 해요.");
                }
                project.reportEvaluation(date);
            }
        }
        ProjectStats.Counts c = stats.of(List.of(projectId), today()).get(projectId);
        return new WorkflowResponse(projectId, project.getEvaluationRequestedDate(), project.getEvaluationReceivedDate(),
                project.getEvaluationReportedDate(), StatusRules.workflowStatus(project), c.participants(), c.clientEvaluated());
    }

    /** 회수: 실제 참여자 전원의 고객평가가 있어야 하고, 회수일은 의뢰일·모든 고객평가일 이상. */
    private void receive(Project project, LocalDate date) {
        List<ProjectMember> participants = EvaluationRules.participants(memberRepository.findTeam(project.getId()));
        Set<Integer> participantIds = participants.stream().map(ProjectMember::getId).collect(Collectors.toSet());
        List<Evaluation> clientEvaluations = evaluationRepository.findByProjects(List.of(project.getId())).stream()
                .filter(ev -> ev.getType() == EvaluationType.CLIENT && participantIds.contains(ev.getTarget().getId()))
                .toList();
        long received = clientEvaluations.stream().map(ev -> ev.getTarget().getId()).distinct().count();
        if (participants.isEmpty() || received < participants.size()) {
            throw new ApiException(ErrorCode.WORKFLOW_CONFLICT,
                    "참여자 전원의 고객평가가 필요해요. 현재 " + received + "/" + participants.size() + "명 등록됐어요.");
        }
        if (date.isBefore(project.getEvaluationRequestedDate())) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "processed_date", "회수일은 의뢰일 이후여야 해요.");
        }
        if (clientEvaluations.stream().anyMatch(ev -> ev.getEvaluatedDate().isAfter(date))) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "processed_date", "회수일은 등록된 모든 고객평가일 이후여야 해요.");
        }
        project.receiveEvaluation(date);
    }

    private static void validateContent(CreateEvaluationRequest r) {
        for (var score : List.of(new Object[] {"work_performance_score", r.workPerformanceScore()},
                new Object[] {"communication_score", r.communicationScore()})) {
            int value = (Integer) score[1];
            if (value < 1 || value > 5) {
                throw ApiException.invalid((String) score[0], "점수는 1~5 사이 정수로 선택해 주세요.");
            }
        }
        for (var comment : List.of(new String[] {"work_performance_comment", r.workPerformanceComment()},
                new String[] {"communication_comment", r.communicationComment()})) {
            if (comment[1].isBlank()) {
                throw ApiException.invalid(comment[0], "평가 의견을 작성해 주세요.");
            }
            if (comment[1].trim().length() > COMMENT_MAX) {
                throw ApiException.invalid(comment[0], "평가 의견은 " + COMMENT_MAX + "자 이하로 작성해 주세요.");
            }
        }
    }

    private LocalDate today() {
        return TimeConfig.today(clock);
    }
}
