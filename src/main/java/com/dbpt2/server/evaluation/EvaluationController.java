package com.dbpt2.server.evaluation;

import com.dbpt2.server.common.ApiResponse;
import com.dbpt2.server.common.Csv;
import com.dbpt2.server.common.Paging;
import com.dbpt2.server.evaluation.EvaluationDtos.CreateEvaluationRequest;
import com.dbpt2.server.evaluation.EvaluationDtos.CreatedEvaluation;
import com.dbpt2.server.evaluation.EvaluationDtos.EligibleProject;
import com.dbpt2.server.evaluation.EvaluationDtos.EvaluationOptions;
import com.dbpt2.server.evaluation.EvaluationDtos.EvaluationResult;
import com.dbpt2.server.evaluation.EvaluationDtos.WorkflowRequest;
import com.dbpt2.server.evaluation.EvaluationDtos.WorkflowResponse;
import com.dbpt2.server.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "평가")
@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationCommandService commandService;
    private final EvaluationQueryService queryService;

    @Operation(summary = "평가 가능한 프로젝트", description = "본인이 실제 참여한 종료 프로젝트와 허용 평가 유형")
    @GetMapping("/api/v1/evaluations/eligible-projects")
    public ApiResponse<List<EligibleProject>> eligible(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.of(queryService.eligibleProjects(user));
    }

    @Operation(summary = "평가 대상 선택", description = "구성원이 아니면 404, 실제 참여자·역할 조건 위반 403, 그 외 제한은 can_submit=false")
    @GetMapping("/api/v1/projects/{projectId}/evaluation-options")
    public ApiResponse<EvaluationOptions> options(@AuthenticationPrincipal AuthUser user, @PathVariable int projectId,
                                                  @RequestParam(name = "evaluation_type") EvaluationType type) {
        return ApiResponse.of(queryService.options(user, projectId, type));
    }

    @Operation(summary = "평가 등록", description = "평가자·등록자는 토큰으로 결정. 동료: 실제 참여자 / 고객·PM: 해당 PM")
    @PostMapping("/api/v1/evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatedEvaluation> create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateEvaluationRequest request) {
        return ApiResponse.of(commandService.create(user, request));
    }

    @Operation(summary = "고객평가 단계 기록", description = "해당 PM. step: REQUEST → RECEIVE → REPORT")
    @PatchMapping("/api/v1/projects/{projectId}/customer-evaluation")
    public ApiResponse<WorkflowResponse> workflow(@AuthenticationPrincipal AuthUser user, @PathVariable int projectId,
                                                  @Valid @RequestBody WorkflowRequest request) {
        return ApiResponse.of(commandService.recordWorkflow(user, projectId, request.step(), request.processedDate()));
    }

    @Operation(summary = "평가 결과 검색", description = "경영진 전체 / 직원 본인 소속 프로젝트. 의견은 경영진·해당 PM만")
    @GetMapping("/api/v1/evaluations")
    public ApiResponse.PageBody<EvaluationResult> search(@AuthenticationPrincipal AuthUser user,
                                                        @RequestParam(name = "target_employee_name", required = false) String name,
                                                        @RequestParam(name = "target_employee_id", required = false) Integer employeeId,
                                                        @RequestParam(name = "project_id", required = false) Integer projectId,
                                                        @RequestParam(name = "evaluation_type", required = false) EvaluationType type,
                                                        @RequestParam(required = false) LocalDate from,
                                                        @RequestParam(required = false) LocalDate to,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size) {
        var filter = new EvaluationQueryService.Filter(name, employeeId, projectId, type, from, to);
        return ApiResponse.page(queryService.search(user, filter, Paging.of(page, size, Sort.unsorted())));
    }

    @Operation(summary = "평가 결과 다운로드", description = "결과 검색과 같은 필터·행·의견 권한. UTF-8 BOM CSV")
    @GetMapping("/api/v1/evaluations/export.csv")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthUser user,
                                         @RequestParam(name = "target_employee_name", required = false) String name,
                                         @RequestParam(name = "target_employee_id", required = false) Integer employeeId,
                                         @RequestParam(name = "project_id", required = false) Integer projectId,
                                         @RequestParam(name = "evaluation_type", required = false) EvaluationType type,
                                         @RequestParam(required = false) LocalDate from,
                                         @RequestParam(required = false) LocalDate to) {
        var filter = new EvaluationQueryService.Filter(name, employeeId, projectId, type, from, to);
        return Csv.download("평가결과.csv", queryService.csv(user, filter));
    }

    @Operation(summary = "평가 결과 상세", description = "결과 조회 범위와 동일. 범위 밖 404")
    @GetMapping("/api/v1/evaluations/{evaluationId}")
    public ApiResponse<EvaluationResult> detail(@AuthenticationPrincipal AuthUser user, @PathVariable int evaluationId) {
        return ApiResponse.of(queryService.detail(user, evaluationId));
    }
}
