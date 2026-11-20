package com.dbpt2.server.dashboard;

import com.dbpt2.server.member.MemberDtos.AssignmentResponse;
import com.dbpt2.server.member.ProjectRole;
import com.dbpt2.server.status.ProjectStatus;
import com.dbpt2.server.status.WorkflowStatus;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {}

    public record DashboardProject(int projectId, String projectName, int clientId, String clientName, LocalDate actualStartDate,
                                   LocalDate plannedEndDate, LocalDate actualEndDate, ProjectStatus status,
                                   long currentMemberCount, boolean overdue) {}

    public record CustomerEvaluationTask(int projectId, String projectName, LocalDate actualEndDate, WorkflowStatus workflowStatus,
                                         long participantCount, long receivedCount) {}

    public record ManagementMetrics(long ongoingProjectCount, long totalProjectCount, long employeesWithoutProjects,
                                    long customerEvaluationPending) {}

    /** 경영진: 기준일 진행 프로젝트·투입 현황 + 현재 기준 참여 없는 재직 직원·고객평가 보고 대기. */
    public record ManagementDashboard(String view, LocalDate asOf, LocalDate serverToday, ManagementMetrics metrics,
                                      List<DashboardProject> ongoingProjects, List<DashboardProject> overdueProjects,
                                      List<CustomerEvaluationTask> pendingCustomerEvaluations,
                                      List<DashboardProject> timelineProjects, List<AssignmentResponse> currentAssignments) {}

    public record MyProject(int projectId, String projectName, int clientId, String clientName, LocalDate actualStartDate,
                            LocalDate plannedEndDate, ProjectRole myRole, LocalDate myAssignmentStartDate,
                            long currentMemberCount, boolean overdue) {}

    public record EvaluationTask(int projectId, String projectName, long count) {}

    public record UpcomingPmProject(int projectId, String projectName, LocalDate plannedEndDate, long daysLeft) {}

    public record EmployeeMetrics(long activeProjectCount, long upcomingProjectCount, long pendingEvaluationCount,
                                  long pendingEvaluationProjectCount) {}

    /** 직원: 내 참여·예정 프로젝트, 미제출 평가, PM이면 고객평가 진행과 D-7 종료 예정. */
    public record EmployeeDashboard(String view, LocalDate asOf, LocalDate serverToday, EmployeeMetrics metrics,
                                    List<MyProject> myProjects, List<EvaluationTask> evaluationTasks,
                                    List<CustomerEvaluationTask> customerEvaluationTasks,
                                    boolean hasCurrentPmAssignment, List<UpcomingPmProject> upcomingPmProjects) {}
}
