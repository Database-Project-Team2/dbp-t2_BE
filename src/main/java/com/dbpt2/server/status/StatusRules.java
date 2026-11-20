package com.dbpt2.server.status;

import com.dbpt2.server.member.ProjectMember;
import com.dbpt2.server.project.Project;
import java.time.LocalDate;

/**
 * 상태 판정 규칙을 한 곳에 둔다. Java 판정과 JPQL 조각이 같은 규칙을 표현하며, 쿼리는 아래 상수를 그대로 쓴다.
 * 별칭: 프로젝트 p, 구성원 m. 기준일 파라미터는 :asOf.
 *
 * <ul>
 *   <li>프로젝트: 착수일 &gt; 기준일이면 착수 전, 실제 종료일 ≤ 기준일이면 종료, 그 외 진행 중(예정 종료일 경과와 무관).</li>
 *   <li>투입: 시작일이 없거나 기준일 이후면 투입 전, 종료일 &lt; 기준일이면 투입 종료, 그 외 투입 중 — 시작·종료일 당일 포함.</li>
 *   <li>실제 참여자: 투입 시작일이 있고, 종료된 프로젝트면 시작일 ≤ 실제 종료일. 평가 대상·평가자 자격의 기준.</li>
 *   <li>참여 중(참여 없음 판정용): 종료되지 않은 프로젝트에서 투입 중이거나 투입 전인 배정.</li>
 * </ul>
 */
public final class StatusRules {

    /** 기준일에 진행 중인 프로젝트. */
    public static final String PROJECT_IN_PROGRESS =
            "(p.actualStartDate <= :asOf AND (p.actualEndDate IS NULL OR p.actualEndDate > :asOf))";

    /** 기준일에 종료되지 않은 프로젝트(착수 전 포함). */
    public static final String PROJECT_NOT_CLOSED = "(p.actualEndDate IS NULL OR p.actualEndDate > :asOf)";

    /** 기준일에 종료된 프로젝트. */
    public static final String PROJECT_CLOSED = "(p.actualEndDate IS NOT NULL AND p.actualEndDate <= :asOf)";

    /** 기준일에 착수 전인 프로젝트. */
    public static final String PROJECT_NOT_STARTED = "(p.actualStartDate > :asOf)";

    /** 프로젝트 수행기간이 기준일을 포함(당일 포함, 종료일 없으면 열린 구간). */
    public static final String PROJECT_PERIOD_CONTAINS =
            "(p.actualStartDate <= :asOf AND (p.actualEndDate IS NULL OR p.actualEndDate >= :asOf))";

    /** 기준일에 투입 중(시작·종료일 당일 포함). */
    public static final String MEMBER_ASSIGNED =
            "(m.assignmentStartDate IS NOT NULL AND m.assignmentStartDate <= :asOf"
                    + " AND (m.assignmentEndDate IS NULL OR m.assignmentEndDate >= :asOf))";

    /** 기준일에 투입이 끝나지 않은 배정(투입 전 포함). */
    public static final String MEMBER_NOT_ENDED = "(m.assignmentEndDate IS NULL OR m.assignmentEndDate >= :asOf)";

    /** 실제 참여자. */
    public static final String ACTUAL_PARTICIPANT =
            "(m.assignmentStartDate IS NOT NULL AND (p.actualEndDate IS NULL OR m.assignmentStartDate <= p.actualEndDate))";

    private StatusRules() {}

    public static ProjectStatus projectStatus(Project p, LocalDate asOf) {
        if (p.getActualStartDate().isAfter(asOf)) {
            return ProjectStatus.NOT_STARTED;
        }
        if (p.getActualEndDate() != null && !p.getActualEndDate().isAfter(asOf)) {
            return ProjectStatus.CLOSED;
        }
        return ProjectStatus.IN_PROGRESS;
    }

    public static AssignmentStatus assignmentStatus(ProjectMember m, LocalDate asOf) {
        if (m.getAssignmentStartDate() == null || m.getAssignmentStartDate().isAfter(asOf)) {
            return AssignmentStatus.NOT_ASSIGNED;
        }
        if (m.getAssignmentEndDate() != null && m.getAssignmentEndDate().isBefore(asOf)) {
            return AssignmentStatus.ENDED;
        }
        return AssignmentStatus.ASSIGNED;
    }

    public static boolean isAssigned(ProjectMember m, LocalDate asOf) {
        return assignmentStatus(m, asOf) == AssignmentStatus.ASSIGNED;
    }

    /** 종료되지 않은 프로젝트에서 투입 중/투입 전인 배정인가. */
    public static boolean isOpenMembership(ProjectMember m, LocalDate asOf) {
        return projectStatus(m.getProject(), asOf) != ProjectStatus.CLOSED
                && (m.getAssignmentEndDate() == null || !m.getAssignmentEndDate().isBefore(asOf));
    }

    public static boolean isActualParticipant(ProjectMember m) {
        LocalDate end = m.getProject().getActualEndDate();
        return m.getAssignmentStartDate() != null && (end == null || !m.getAssignmentStartDate().isAfter(end));
    }

    public static WorkflowStatus workflowStatus(Project p) {
        if (p.getEvaluationReportedDate() != null) {
            return WorkflowStatus.REPORTED;
        }
        if (p.getEvaluationReceivedDate() != null) {
            return WorkflowStatus.AWAITING_REPORT;
        }
        if (p.getEvaluationRequestedDate() != null) {
            return WorkflowStatus.COLLECTING;
        }
        return WorkflowStatus.NOT_REQUESTED;
    }

    /** 기간 검색: [from, to]와 겹치는가. 시작이 없으면(투입 전) 겹치지 않는다. 종료일 null은 열린 구간. */
    public static boolean overlaps(LocalDate start, LocalDate end, LocalDate from, LocalDate to) {
        return start != null && (from == null || end == null || !end.isBefore(from)) && (to == null || !start.isAfter(to));
    }
}
