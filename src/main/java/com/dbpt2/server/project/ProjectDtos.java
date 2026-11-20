package com.dbpt2.server.project;

import com.dbpt2.server.client.Client;
import com.dbpt2.server.member.ProjectRole;
import com.dbpt2.server.status.AssignmentStatus;
import com.dbpt2.server.status.ProjectStatus;
import com.dbpt2.server.status.WorkflowStatus;
import java.time.LocalDate;

public final class ProjectDtos {

    private ProjectDtos() {}

    public record ClientRef(int clientId, String clientName) {
        public static ClientRef of(Client c) {
            return new ClientRef(c.getId(), c.getName());
        }
    }

    public record ClientContact(int clientId, String clientName, String contactName, String contactEmail, String contactPhoneNumber) {
        public static ClientContact of(Client c) {
            return new ClientContact(c.getId(), c.getName(), c.getContactName(), c.getContactEmail(), c.getContactPhoneNumber());
        }
    }

    /** 목록 항목: 기본 응답 + status, workflow_status, client, member_count, current_member_count. */
    public record ProjectListItem(int projectId, String projectName, int clientId, LocalDate actualStartDate,
                                  LocalDate plannedEndDate, LocalDate actualEndDate, LocalDate evaluationRequestedDate,
                                  LocalDate evaluationReceivedDate, LocalDate evaluationReportedDate,
                                  ProjectStatus status, WorkflowStatus workflowStatus, ClientRef client,
                                  long memberCount, long currentMemberCount) {}

    public record MyMembership(int memberId, ProjectRole projectRole, LocalDate assignmentStartDate,
                               LocalDate assignmentEndDate, AssignmentStatus assignmentStatus, boolean actualParticipant) {}

    /** 화면 버튼 표시용. 실제 권한은 각 API가 다시 검사한다. */
    public record Permissions(boolean canManage, boolean canRecordCustomerEvaluation, boolean canViewEvaluationComments,
                              boolean canEvaluate) {}

    public record CustomerEvaluationProgress(long participantCount, long receivedCount) {}

    /** 상세: 기본 응답 + 발주처 연락처 + 본인 역할 + permissions + 고객평가 집계. */
    public record ProjectDetail(int projectId, String projectName, int clientId, LocalDate actualStartDate,
                                LocalDate plannedEndDate, LocalDate actualEndDate, LocalDate evaluationRequestedDate,
                                LocalDate evaluationReceivedDate, LocalDate evaluationReportedDate,
                                ProjectStatus status, WorkflowStatus workflowStatus, ClientContact client,
                                long memberCount, long currentMemberCount, MyMembership myMembership,
                                Permissions permissions, CustomerEvaluationProgress customerEvaluationProgress) {}
}
