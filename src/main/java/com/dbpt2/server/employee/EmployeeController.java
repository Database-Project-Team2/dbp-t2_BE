package com.dbpt2.server.employee;

import com.dbpt2.server.auth.AuthResponses.TokenResponse;
import com.dbpt2.server.auth.AuthService;
import com.dbpt2.server.common.ApiResponse;
import com.dbpt2.server.common.Paging;
import com.dbpt2.server.employee.EmployeeQueryService.EmployeeListItem;
import com.dbpt2.server.security.AuthUser;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "직원")
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRegistrationService registrationService;
    private final AuthService authService;
    private final EmployeeQueryService queryService;

    @Operation(summary = "회원가입", description = "공개. 모든 가입자는 EMPLOYEE, 사번은 서버 발급. system_role·employee_no를 보내면 400")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.of(authService.tokenFor(registrationService.register(request)));
    }

    @Operation(summary = "직원 검색", description = "경영진 전용. skill은 여러 번 보내면 AND(부분 일치). assigned는 오늘 투입 중 여부")
    @GetMapping
    public ApiResponse.PageBody<EmployeeListItem> search(@AuthenticationPrincipal AuthUser user,
                                                        @RequestParam(name = "employee_name", required = false) String employeeName,
                                                        @RequestParam(name = "employee_no", required = false) String employeeNo,
                                                        @RequestParam(name = "job_category", required = false) JobCategory jobCategory,
                                                        @RequestParam(name = "employment_status", required = false) EmploymentStatus employmentStatus,
                                                        @RequestParam(required = false) Boolean assigned,
                                                        @RequestParam(name = "min_career_months", required = false) Integer minCareerMonths,
                                                        @RequestParam(name = "skill", required = false) List<String> skills,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size) {
        var criteria = new EmployeeSearch.Criteria(employeeName, employeeNo, jobCategory, employmentStatus, assigned,
                minCareerMonths, skills, queryService.today(), null, false);
        return ApiResponse.page(queryService.search(user, criteria, Paging.of(page, size, Sort.unsorted())));
    }

    @Operation(summary = "직원 상세", description = "경영진 또는 본인. 그 외 404")
    @GetMapping("/{employeeId}")
    public ApiResponse<EmployeeResponse> detail(@AuthenticationPrincipal AuthUser user, @PathVariable int employeeId) {
        return ApiResponse.of(queryService.detail(user, employeeId));
    }
}
