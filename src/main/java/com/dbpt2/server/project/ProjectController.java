package com.dbpt2.server.project;

import com.dbpt2.server.common.ApiResponse;
import com.dbpt2.server.common.Paging;
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
}
