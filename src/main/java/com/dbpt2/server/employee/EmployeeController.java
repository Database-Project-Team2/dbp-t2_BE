package com.dbpt2.server.employee;

import com.dbpt2.server.auth.AuthResponses.TokenResponse;
import com.dbpt2.server.auth.AuthService;
import com.dbpt2.server.common.ApiResponse;
import com.dbpt2.server.common.Csv;
import com.dbpt2.server.common.Paging;
import com.dbpt2.server.common.PatchBody;
import com.dbpt2.server.member.AssignmentQueryService;
import com.dbpt2.server.member.CareerService;
import com.dbpt2.server.member.MemberDtos.AssignmentResponse;
import com.dbpt2.server.member.ProjectRole;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final ProfileService profileService;
    private final CareerService careerService;

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

    @Operation(summary = "내 정보 수정", description = "본인. education?, total_career_months?, skill_description?만 수정")
    @PatchMapping("/me")
    public ApiResponse<EmployeeResponse> updateMe(@AuthenticationPrincipal AuthUser user, @RequestBody JsonNode body) {
        return ApiResponse.of(profileService.updateProfile(user,
                new PatchBody(body, Set.of("education", "total_career_months", "skill_description"))));
    }

    @Operation(summary = "비밀번호 변경", description = "본인. 현재 비밀번호 확인 후 변경, 204. 기존 토큰은 폐기하지 않음")
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthUser user, @RequestBody ProfileService.PasswordRequest request) {
        profileService.changePassword(user, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "개인 경력", description = "경영진 또는 본인(본인은 실제 투입 이력만). 그 외 404")
    @GetMapping("/{employeeId}/career")
    public ApiResponse<List<AssignmentResponse>> career(@AuthenticationPrincipal AuthUser user, @PathVariable int employeeId,
                                                        @RequestParam(name = "project_id", required = false) Integer projectId,
                                                        @RequestParam(name = "project_role", required = false) ProjectRole projectRole,
                                                        @RequestParam(name = "as_of", required = false) LocalDate asOf,
                                                        @RequestParam(required = false) LocalDate from,
                                                        @RequestParam(required = false) LocalDate to,
                                                        @RequestParam(name = "ongoing_projects_only", required = false, defaultValue = "false") boolean ongoing) {
        var filter = new AssignmentQueryService.Filter(null, employeeId, projectId, projectRole, asOf, from, to, ongoing);
        return ApiResponse.of(careerService.career(user, employeeId, filter));
    }

    @Operation(summary = "경력 다운로드", description = "개인 경력과 같은 필터·권한. UTF-8 BOM CSV")
    @GetMapping("/{employeeId}/career.csv")
    public ResponseEntity<byte[]> careerCsv(@AuthenticationPrincipal AuthUser user, @PathVariable int employeeId,
                                            @RequestParam(name = "project_id", required = false) Integer projectId,
                                            @RequestParam(name = "project_role", required = false) ProjectRole projectRole,
                                            @RequestParam(name = "as_of", required = false) LocalDate asOf,
                                            @RequestParam(required = false) LocalDate from,
                                            @RequestParam(required = false) LocalDate to,
                                            @RequestParam(name = "ongoing_projects_only", required = false, defaultValue = "false") boolean ongoing) {
        var filter = new AssignmentQueryService.Filter(null, employeeId, projectId, projectRole, asOf, from, to, ongoing);
        String content = careerService.careerCsv(user, employeeId, filter);
        return Csv.download(careerService.careerFilename(employeeId), content);
    }
}
