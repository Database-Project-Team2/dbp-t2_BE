package com.dbpt2.server.evaluation;

import com.dbpt2.server.member.ProjectMember;
import com.dbpt2.server.member.ProjectRole;
import com.dbpt2.server.status.WorkflowStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public final class EvaluationDtos {

    private EvaluationDtos() {}

    /** 평가 등록 입력. 평가자·등록자는 받지 않는다(토큰으로 서버가 결정). 모두 필수. */
    public record CreateEvaluationRequest(
            @NotNull(message = "평가 대상을 선택해 주세요.") Integer targetMemberId,
            @NotNull(message = "평가유형을 선택해 주세요.") EvaluationType evaluationType,
            @NotNull(message = "평가일을 입력해 주세요.") LocalDate evaluatedDate,
            @NotNull(message = "업무수행 점수를 선택해 주세요.") Integer workPerformanceScore,
            @NotNull(message = "업무수행 의견을 작성해 주세요.") String workPerformanceComment,
            @NotNull(message = "커뮤니케이션 점수를 선택해 주세요.") Integer communicationScore,
            @NotNull(message = "커뮤니케이션 의견을 작성해 주세요.") String communicationComment) {}

    public record CreatedEvaluation(int evaluationId, int projectId, int targetMemberId, EvaluationType evaluationType,
                                    LocalDate evaluatedDate) {}

    /** 평가 응답의 구성원 요약(직원 + 이 프로젝트 역할). */
    public record Person(int memberId, int employeeId, String employeeNo, String employeeName, ProjectRole projectRole) {
        public static Person of(ProjectMember m) {
            return new Person(m.getId(), m.getEmployee().getId(), m.getEmployee().getEmployeeNo(), m.getEmployee().getName(), m.getRole());
        }
    }

    /** 실제 평가자. 고객평가는 발주처의 현재 대표 담당자(이력 미보관). */
    public record Evaluator(String name, Integer employeeId, boolean clientContact, String clientName) {}

    /** 결과 행. 의견 두 필드는 경영진·해당 프로젝트 PM에게만 넣고, 아니면 키 자체를 뺀다. */
    public record EvaluationResult(int evaluationId, int projectId, String projectName, int targetMemberId, Person target,
                                   EvaluationType evaluationType, Integer evaluatorMemberId, Evaluator evaluator,
                                   int registeredByMemberId, Person registeredBy, LocalDate evaluatedDate,
                                   int workPerformanceScore,
                                   @JsonInclude(JsonInclude.Include.NON_NULL) String workPerformanceComment,
                                   int communicationScore,
                                   @JsonInclude(JsonInclude.Include.NON_NULL) String communicationComment,
                                   boolean commentsVisible) {}

    public record EligibleProject(int projectId, String projectName, LocalDate actualEndDate, int myMemberId, ProjectRole myRole,
                                  List<EvaluationType> allowedTypes, long remainingCount, long submittedCount,
                                  WorkflowStatus workflowStatus) {}

    public record EvaluationOptions(int projectId, String projectName, EvaluationType evaluationType, boolean canSubmit,
                                    String blockedReason, Evaluator evaluator, Person registeredBy,
                                    List<Person> availableTargets, LocalDate minEvaluatedDate, LocalDate maxEvaluatedDate) {}

    public enum WorkflowStep { REQUEST, RECEIVE, REPORT }

    public record WorkflowRequest(@NotNull(message = "처리 단계를 선택해 주세요.") WorkflowStep step,
                                  @NotNull(message = "처리일을 입력해 주세요.") LocalDate processedDate) {}

    public record WorkflowResponse(int projectId, LocalDate evaluationRequestedDate, LocalDate evaluationReceivedDate,
                                   LocalDate evaluationReportedDate, WorkflowStatus workflowStatus, long participantCount,
                                   long receivedCount) {}
}
