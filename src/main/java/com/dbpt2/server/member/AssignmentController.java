package com.dbpt2.server.member;

import com.dbpt2.server.common.ApiResponse;
import com.dbpt2.server.common.Csv;
import org.springframework.http.ResponseEntity;
import com.dbpt2.server.common.Paging;
import com.dbpt2.server.member.MemberDtos.AssignmentResponse;
import com.dbpt2.server.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "투입 이력")
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentQueryService queryService;
    private final CareerService careerService;

    @Operation(summary = "투입 이력 검색", description = "경영진 전용. 기준일은 시작·종료일 당일 포함, 기간은 겹치는 구간")
    @GetMapping
    public ApiResponse.PageBody<AssignmentResponse> search(@AuthenticationPrincipal AuthUser user,
                                                          @RequestParam(name = "employee_name", required = false) String employeeName,
                                                          @RequestParam(name = "employee_id", required = false) Integer employeeId,
                                                          @RequestParam(name = "project_id", required = false) Integer projectId,
                                                          @RequestParam(name = "project_role", required = false) ProjectRole projectRole,
                                                          @RequestParam(name = "as_of", required = false) LocalDate asOf,
                                                          @RequestParam(required = false) LocalDate from,
                                                          @RequestParam(required = false) LocalDate to,
                                                          @RequestParam(name = "ongoing_projects_only", required = false, defaultValue = "false") boolean ongoing,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        var filter = new AssignmentQueryService.Filter(employeeName, employeeId, projectId, projectRole, asOf, from, to, ongoing);
        return ApiResponse.page(queryService.search(user, filter, Paging.of(page, size, Sort.unsorted())));
    }

    @Operation(summary = "투입 이력 다운로드", description = "경영진 전용. 이력 검색과 같은 필터, 전체 결과 CSV(UTF-8 BOM)")
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthUser user,
                                         @RequestParam(name = "employee_name", required = false) String employeeName,
                                         @RequestParam(name = "employee_id", required = false) Integer employeeId,
                                         @RequestParam(name = "project_id", required = false) Integer projectId,
                                         @RequestParam(name = "project_role", required = false) ProjectRole projectRole,
                                         @RequestParam(name = "as_of", required = false) LocalDate asOf,
                                         @RequestParam(required = false) LocalDate from,
                                         @RequestParam(required = false) LocalDate to,
                                         @RequestParam(name = "ongoing_projects_only", required = false, defaultValue = "false") boolean ongoing) {
        var filter = new AssignmentQueryService.Filter(employeeName, employeeId, projectId, projectRole, asOf, from, to, ongoing);
        return Csv.download("프로젝트_투입이력.csv", careerService.assignmentsCsv(queryService.searchAll(user, filter)));
    }
}
