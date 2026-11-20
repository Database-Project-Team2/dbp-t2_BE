package com.dbpt2.server.client;

import com.dbpt2.server.common.ApiResponse;
import com.dbpt2.server.common.Paging;
import com.dbpt2.server.common.PatchBody;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.dbpt2.server.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "발주처")
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientQueryService queryService;
    private final ClientCommandService commandService;

    @Operation(summary = "발주처 검색", description = "경영진 전체 / 직원 본인 소속 프로젝트의 발주처. query는 회사명 또는 담당자명")
    @GetMapping
    public ApiResponse.PageBody<ClientResponse> search(@AuthenticationPrincipal AuthUser user,
                                                      @RequestParam(required = false) String query,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size) {
        return ApiResponse.page(queryService.search(user, query, Paging.of(page, size, Sort.unsorted())));
    }

    @Operation(summary = "발주처 상세", description = "경영진 또는 해당 발주처 프로젝트 구성원. 범위 밖 404")
    @GetMapping("/{clientId}")
    public ApiResponse<ClientResponse> detail(@AuthenticationPrincipal AuthUser user, @PathVariable int clientId) {
        return ApiResponse.of(queryService.detail(user, clientId));
    }

    @Operation(summary = "발주처 등록", description = "경영진 전용")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClientResponse> create(@AuthenticationPrincipal AuthUser user,
                                              @RequestBody ClientCommandService.CreateClientRequest request) {
        return ApiResponse.of(queryService.detail(user, commandService.create(user, request)));
    }

    @Operation(summary = "발주처 수정", description = "경영진 전용. 보낸 필드만 수정, contact_phone_number는 null로 비우기 가능")
    @PatchMapping("/{clientId}")
    public ApiResponse<ClientResponse> update(@AuthenticationPrincipal AuthUser user, @PathVariable int clientId,
                                              @RequestBody JsonNode body) {
        commandService.update(user, clientId,
                new PatchBody(body, Set.of("client_name", "contact_name", "contact_email", "contact_phone_number")));
        return ApiResponse.of(queryService.detail(user, clientId));
    }
}
