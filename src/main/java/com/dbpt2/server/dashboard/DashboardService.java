package com.dbpt2.server.dashboard;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.dashboard.DashboardDtos.CustomerEvaluationTask;
import com.dbpt2.server.dashboard.DashboardDtos.DashboardProject;
import com.dbpt2.server.dashboard.DashboardDtos.EmployeeDashboard;
import com.dbpt2.server.dashboard.DashboardDtos.EmployeeMetrics;
import com.dbpt2.server.dashboard.DashboardDtos.EvaluationTask;
import com.dbpt2.server.dashboard.DashboardDtos.ManagementDashboard;
import com.dbpt2.server.dashboard.DashboardDtos.ManagementMetrics;
import com.dbpt2.server.dashboard.DashboardDtos.MyProject;
import com.dbpt2.server.dashboard.DashboardDtos.UpcomingPmProject;
import com.dbpt2.server.employee.EmployeeRepository;
import com.dbpt2.server.evaluation.Evaluation;
import com.dbpt2.server.evaluation.EvaluationRepository;
import com.dbpt2.server.evaluation.EvaluationRules;
import com.dbpt2.server.evaluation.EvaluationType;
import com.dbpt2.server.member.MemberDtos.AssignmentResponse;
import com.dbpt2.server.member.ProjectMember;
import com.dbpt2.server.member.ProjectMemberRepository;
import com.dbpt2.server.member.ProjectRole;
import com.dbpt2.server.project.Project;
import com.dbpt2.server.project.ProjectRepository;
import com.dbpt2.server.project.ProjectStats;
import com.dbpt2.server.security.AuthUser;
import com.dbpt2.server.status.ProjectStatus;
import com.dbpt2.server.status.StatusRules;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    static final int PM_NOTICE_DAYS = 7;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final EmployeeRepository employeeRepository;
    private final EvaluationRepository evaluationRepository;
    private final ProjectStats stats;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public Object dashboard(AuthUser user, LocalDate asOf) {
        LocalDate today = TimeConfig.today(clock);
        LocalDate date = asOf != null ? asOf : today;
        return accessPolicy.isManagement(user) ? management(date, today) : employee(user, date, today);
    }

    ManagementDashboard management(LocalDate asOf, LocalDate today) {
        List<Project> ongoing = projectRepository.findInProgress(asOf);
        List<Project> pending = projectRepository.findPendingCustomerEvaluation();
        List<Project> all = projectRepository.findAllWithClient();
        Map<Integer, ProjectStats.Counts> counts = stats.of(all.stream().map(Project::getId).toList(), asOf);
        List<DashboardProject> ongoingItems = ongoing.stream().map(p -> project(p, counts.get(p.getId()), asOf)).toList();
        List<CustomerEvaluationTask> pendingItems = pending.stream().map(p -> customerTask(p, counts.get(p.getId()))).toList();
        // 연간 일정: 서버 기준일이 속한 해에 수행기간이 걸친 프로젝트.
        int year = today.getYear();
        List<DashboardProject> timeline = all.stream()
                .filter(p -> p.getActualStartDate().getYear() <= year && (p.getActualEndDate() == null || p.getActualEndDate().getYear() >= year))
                .map(p -> project(p, counts.get(p.getId()), today)).toList();
        List<AssignmentResponse> assignments = memberRepository.findAssignedInOngoingProjects(asOf).stream()
                .map(m -> AssignmentResponse.of(m, asOf)).toList();
        ManagementMetrics metrics = new ManagementMetrics(projectRepository.countInProgress(asOf), all.size(),
                employeeRepository.countActiveWithoutProjects(today), pending.size());
        return new ManagementDashboard("MANAGEMENT", asOf, today, metrics, ongoingItems,
                ongoingItems.stream().filter(DashboardProject::overdue).toList(), pendingItems, timeline, assignments);
    }

    EmployeeDashboard employee(AuthUser user, LocalDate asOf, LocalDate today) {
        List<ProjectMember> open = memberRepository.findOpenMemberships(user.employeeId(), asOf);
        Map<Integer, ProjectStats.Counts> counts = stats.of(open.stream().map(m -> m.getProject().getId()).toList(), asOf);
        List<MyProject> myProjects = open.stream().map(m -> {
            Project p = m.getProject();
            ProjectStats.Counts c = counts.get(p.getId());
            return new MyProject(p.getId(), p.getName(), p.getClient().getId(), p.getClient().getName(), p.getActualStartDate(),
                    p.getPlannedEndDate(), m.getRole(), m.getAssignmentStartDate(), c == null ? 0 : c.current(),
                    p.getPlannedEndDate().isBefore(asOf));
        }).toList();
        long active = open.stream().filter(m -> m.getAssignmentStartDate() != null && !m.getAssignmentStartDate().isAfter(asOf)).count();

        // 미제출 평가: 본인이 구성원인 종료 프로젝트별로 유형마다 남은 대상 수를 더한다.
        List<ProjectMember> mine = memberRepository.findByEmployee(user.employeeId());
        List<Integer> closedIds = mine.stream().map(ProjectMember::getProject).filter(p -> p.getActualEndDate() != null)
                .map(Project::getId).distinct().toList();
        Map<Integer, List<Evaluation>> evaluations = closedIds.isEmpty() ? Map.of()
                : evaluationRepository.findByProjects(closedIds).stream().collect(Collectors.groupingBy(ev -> ev.getTarget().getProject().getId()));
        List<EvaluationTask> evaluationTasks = new ArrayList<>();
        for (Integer projectId : closedIds) {
            List<ProjectMember> team = memberRepository.findTeam(projectId);
            Project p = team.get(0).getProject();
            long count = 0;
            for (EvaluationType type : List.of(EvaluationType.PEER, EvaluationType.PM, EvaluationType.CLIENT)) {
                count += EvaluationRules.availableTargets(p, team, evaluations.getOrDefault(projectId, List.of()), user.employeeId(), type).size();
            }
            if (count > 0) {
                evaluationTasks.add(new EvaluationTask(projectId, p.getName(), count));
            }
        }

        // PM 담당 고객평가 진행: 해당 프로젝트 PM이고, 종료됐지만 보고 전.
        List<Project> pmClosed = mine.stream().filter(m -> m.getRole() == ProjectRole.PM).map(ProjectMember::getProject)
                .filter(p -> p.getActualEndDate() != null && p.getEvaluationReportedDate() == null).toList();
        Map<Integer, ProjectStats.Counts> pmCounts = stats.of(pmClosed.stream().map(Project::getId).toList(), asOf);
        List<CustomerEvaluationTask> customerTasks = pmClosed.stream().map(p -> customerTask(p, pmCounts.get(p.getId()))).toList();

        // D-7: 참여 중인 PM 배정 중 예정 종료일이 오늘~7일 후. 종료·기한 경과·본인 투입 종료는 open 목록에서 이미 빠진다.
        List<ProjectMember> currentPm = open.stream().filter(m -> m.getRole() == ProjectRole.PM).toList();
        List<UpcomingPmProject> upcoming = currentPm.stream()
                .map(m -> new UpcomingPmProject(m.getProject().getId(), m.getProject().getName(), m.getProject().getPlannedEndDate(),
                        ChronoUnit.DAYS.between(asOf, m.getProject().getPlannedEndDate())))
                .filter(u -> u.daysLeft() >= 0 && u.daysLeft() <= PM_NOTICE_DAYS)
                .sorted(Comparator.comparingLong(UpcomingPmProject::daysLeft).thenComparingInt(UpcomingPmProject::projectId))
                .toList();

        EmployeeMetrics metrics = new EmployeeMetrics(active, open.size() - active,
                evaluationTasks.stream().mapToLong(EvaluationTask::count).sum(), evaluationTasks.size());
        return new EmployeeDashboard("EMPLOYEE", asOf, today, metrics, myProjects, evaluationTasks, customerTasks,
                !currentPm.isEmpty(), upcoming);
    }

    private static DashboardProject project(Project p, ProjectStats.Counts c, LocalDate asOf) {
        return new DashboardProject(p.getId(), p.getName(), p.getClient().getId(), p.getClient().getName(), p.getActualStartDate(),
                p.getPlannedEndDate(), p.getActualEndDate(), StatusRules.projectStatus(p, asOf), c == null ? 0 : c.current(),
                // 기준일에 아직 진행 중인데 예정 종료일이 지났으면 경과(나중에 종료된 프로젝트도 당시에는 경과).
                StatusRules.projectStatus(p, asOf) == ProjectStatus.IN_PROGRESS && p.getPlannedEndDate().isBefore(asOf));
    }

    private static CustomerEvaluationTask customerTask(Project p, ProjectStats.Counts c) {
        return new CustomerEvaluationTask(p.getId(), p.getName(), p.getActualEndDate(), StatusRules.workflowStatus(p),
                c == null ? 0 : c.participants(), c == null ? 0 : c.clientEvaluated());
    }
}
