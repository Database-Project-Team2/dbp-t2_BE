package com.dbpt2.server.evaluation;

import com.dbpt2.server.member.ProjectMember;
import com.dbpt2.server.member.ProjectRole;
import com.dbpt2.server.project.Project;
import com.dbpt2.server.status.StatusRules;
import java.util.List;
import java.util.Optional;

/**
 * 평가 자격 규칙(명세 5장). 프로젝트 한 개의 구성원·평가를 받아 계산하는 순수 함수로 둔다.
 * <ul>
 *   <li>모든 평가는 프로젝트 실제 종료 후, 실제 참여자 사이에서만.</li>
 *   <li>CLIENT: 해당 PM이 등록, 대상은 실제 참여자 전체(PM 자신 포함), 대상당 1건, 의뢰 후·보고 전.</li>
 *   <li>PM: 해당 PM이 작성, 대상은 PM을 제외한 실제 참여자.</li>
 *   <li>PEER: 실제 참여자가 작성, 대상은 본인을 제외한 실제 참여자.</li>
 *   <li>내부평가는 같은 대상·유형·평가자 조합 1건.</li>
 * </ul>
 */
public final class EvaluationRules {

    private EvaluationRules() {}

    public static List<ProjectMember> participants(List<ProjectMember> team) {
        return team.stream().filter(StatusRules::isActualParticipant).toList();
    }

    public static Optional<ProjectMember> ownParticipant(List<ProjectMember> team, int employeeId) {
        return participants(team).stream().filter(m -> m.getEmployee().getId() == employeeId).findFirst();
    }

    /** 평가할 수 없는 이유. 없으면 null. */
    public static String blockedReason(Project project, List<ProjectMember> team, int employeeId, EvaluationType type) {
        if (project.getActualEndDate() == null) {
            return "프로젝트가 종료된 후 평가할 수 있어요.";
        }
        Optional<ProjectMember> own = ownParticipant(team, employeeId);
        if (own.isEmpty()) {
            return "실제 투입 이력이 있는 구성원만 평가에 참여할 수 있어요.";
        }
        if (type != EvaluationType.PEER && own.get().getRole() != ProjectRole.PM) {
            return type == EvaluationType.CLIENT ? "고객평가는 해당 프로젝트의 PM만 등록할 수 있어요."
                    : "PM 평가는 해당 프로젝트의 PM만 작성할 수 있어요.";
        }
        if (type == EvaluationType.CLIENT && project.getEvaluationRequestedDate() == null) {
            return "고객평가 의뢰일을 먼저 기록해 주세요.";
        }
        if (type == EvaluationType.CLIENT && project.getEvaluationReportedDate() != null) {
            return "보고를 완료한 고객평가는 추가할 수 없어요.";
        }
        return null;
    }

    /** 아직 평가하지 않은 대상. 자격이 없으면 빈 목록. */
    public static List<ProjectMember> availableTargets(Project project, List<ProjectMember> team, List<Evaluation> evaluations,
                                                       int employeeId, EvaluationType type) {
        if (blockedReason(project, team, employeeId, type) != null) {
            return List.of();
        }
        ProjectMember own = ownParticipant(team, employeeId).orElseThrow();
        return participants(team).stream()
                .filter(m -> type == EvaluationType.CLIENT || m.getId().intValue() != own.getId())
                .filter(m -> type != EvaluationType.PM || m.getRole() != ProjectRole.PM)
                .filter(m -> evaluations.stream().noneMatch(ev -> ev.getTarget().getId().equals(m.getId())
                        && ev.getType() == type
                        && (type == EvaluationType.CLIENT || (ev.getEvaluator() != null && ev.getEvaluator().getId().equals(own.getId())))))
                .toList();
    }
}
