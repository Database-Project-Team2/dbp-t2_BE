package com.dbpt2.server.project;

import com.dbpt2.server.common.ApiResponse;
import com.dbpt2.server.common.Paging;
import com.dbpt2.server.common.PatchBody;
import com.dbpt2.server.member.MemberCandidate;
import com.dbpt2.server.member.MemberCommandService;
import com.dbpt2.server.employee.EmployeeSearch;
import com.dbpt2.server.employee.JobCategory;
import com.dbpt2.server.project.ProjectCommands.ClientOption;
import com.dbpt2.server.project.ProjectCommands.CloseProjectRequest;
import com.dbpt2.server.project.ProjectCommands.CloseProjectResponse;
import com.dbpt2.server.project.ProjectCommands.CreateProjectRequest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.dbpt2.server.member.MemberDtos.MemberResponse;
import com.dbpt2.server.project.ProjectDtos.ProjectDetail;
import com.dbpt2.server.project.ProjectDtos.ProjectListItem;
import com.dbpt2.server.security.AuthUser;
import com.dbpt2.server.status.ProjectStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로젝트")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectQueryService queryService;
    private final ProjectCommandService commandService;
    private final MemberCommandService memberCommandService;
    private final MemberCandidateService candidateService;

    /** 목록 응답에 상태 탭 건수(status_counts)를 meta 옆에 함께 준다. */
    public record ProjectPage(List<ProjectListItem> data, ApiResponse.PageInfo page, Map<ProjectStatus, Long> statusCounts,
                              ApiResponse.Meta meta) {}

    @Operation(summary = "프로젝트 목록·검색", description = "경영진 전체 / 직원 본인 소속(투입 전 포함)")
    @GetMapping
    public ProjectPage list(@AuthenticationPrincipal AuthUser user,
                            @RequestParam(name = "project_name", required = false) String projectName,
                            @RequestParam(name = "client_name", required = false) String clientName,
                            @RequestParam(required = false) ProjectStatus status,
                            @RequestParam(name = "as_of", required = false) LocalDate asOf,
                            @RequestParam(required = false) LocalDate from,
                            @RequestParam(required = false) LocalDate to,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(required = false) Integer size) {
        var result = queryService.list(user, projectName, clientName, status, asOf, from, to, Paging.of(page, size, Sort.unsorted()));
        var body = ApiResponse.page(result.page());
        return new ProjectPage(body.data(), body.page(), result.statusCounts(), body.meta());
    }

    @Operation(summary = "프로젝트 상세", description = "경영진 또는 프로젝트 구성원. 범위 밖은 404")
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetail> detail(@AuthenticationPrincipal AuthUser user, @PathVariable int projectId) {
        return ApiResponse.of(queryService.detail(user, projectId));
    }

    @Operation(summary = "구성원 목록", description = "경영진 또는 프로젝트 구성원")
    @GetMapping("/{projectId}/members")
    public ApiResponse<List<MemberResponse>> members(@AuthenticationPrincipal AuthUser user, @PathVariable int projectId,
                                                     @RequestParam(name = "as_of", required = false) LocalDate asOf) {
        return ApiResponse.of(queryService.members(user, projectId, asOf));
    }

    @Operation(summary = "프로젝트 등록", description = "경영진 전용. 실제 착수일 ≤ 오늘, 예정 종료일 ≥ 착수일")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectDetail> create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.of(queryService.detail(user, commandService.create(user, request)));
    }

    @Operation(summary = "프로젝트 수정", description = "경영진 전용. project_name?, client_id?, actual_start_date?, planned_end_date?. 종료된 프로젝트 409")
    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectDetail> update(@AuthenticationPrincipal AuthUser user, @PathVariable int projectId,
                                             @RequestBody JsonNode body) {
        commandService.update(user, projectId,
                new PatchBody(body, Set.of("project_name", "client_id", "actual_start_date", "planned_end_date")));
        return ApiResponse.of(queryService.detail(user, projectId));
    }

    @Operation(summary = "프로젝트 종료", description = "경영진 전용. 투입 중 구성원 종료일을 함께 기록, 투입 전 구성원 유지")
    @PostMapping("/{projectId}/close")
    public ApiResponse<CloseProjectResponse> close(@AuthenticationPrincipal AuthUser user, @PathVariable int projectId,
                                                   @Valid @RequestBody CloseProjectRequest request) {
        var closed = commandService.close(user, projectId, request.actualEndDate());
        return ApiResponse.of(new CloseProjectResponse(queryService.detail(user, projectId), closed));
    }

    @Operation(summary = "수정용 발주처 선택", description = "경영진 전용")
    @GetMapping("/{projectId}/client-options")
    public ApiResponse<List<ClientOption>> clientOptions(@AuthenticationPrincipal AuthUser user, @PathVariable int projectId,
                                                         @RequestParam(name = "client_name", required = false) String clientName) {
        return ApiResponse.of(commandService.clientOptions(user, projectId, clientName));
    }

    @Operation(summary = "추가 가능한 직원", description = "경영진 전용. ACTIVE이고 이 프로젝트에 없는 직원(다른 프로젝트 참여 여부 무관)")
    @GetMapping("/{projectId}/member-candidates")
    public ApiResponse.PageBody<MemberCandidate> candidates(@AuthenticationPrincipal AuthUser user, @PathVariable int projectId,
                                                           @RequestParam(name = "employee_name", required = false) String employeeName,
                                                           @RequestParam(name = "employee_no", required = false) String employeeNo,
                                                           @RequestParam(name = "job_category", required = false) JobCategory jobCategory,
                                                           @RequestParam(name = "skill", required = false) List<String> skills,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size) {
        var criteria = new EmployeeSearch.Criteria(employeeName, employeeNo, jobCategory, null, null, null, skills, null, projectId, true);
        return ApiResponse.page(candidateService.candidates(user, projectId, criteria, Paging.of(page, size, Sort.unsorted())));
    }

    @Operation(summary = "구성원 추가", description = "경영진 전용. 중복·재투입 409, 날짜 조건 위반 422, ACTIVE만")
    @PostMapping("/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> addMember(@AuthenticationPrincipal AuthUser user, @PathVariable int projectId,
                                                 @Valid @RequestBody MemberCommandService.AddMemberRequest request) {
        return ApiResponse.of(memberCommandService.add(user, projectId, request));
    }
}
