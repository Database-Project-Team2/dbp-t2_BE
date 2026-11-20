package com.dbpt2.server.member;

import com.dbpt2.server.common.ApiResponse;
import com.dbpt2.server.common.PatchBody;
import com.dbpt2.server.member.MemberDtos.MemberResponse;
import com.dbpt2.server.security.AuthUser;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로젝트 구성원")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberCommandService commandService;

    @Operation(summary = "투입기간 수정", description = "경영진 전용. assignment_start_date?, assignment_end_date? (null로 비우기 가능, 투입 후 시작일 삭제 불가)")
    @PatchMapping("/{memberId}/assignment")
    public ApiResponse<MemberResponse> changeAssignment(@AuthenticationPrincipal AuthUser user, @PathVariable int memberId,
                                                        @RequestBody JsonNode body) {
        PatchBody patch = new PatchBody(body, Set.of("assignment_start_date", "assignment_end_date"));
        return ApiResponse.of(commandService.changeAssignment(user, memberId, patch));
    }

    @Operation(summary = "투입 전 구성원 삭제", description = "경영진 전용. 시작·종료일이 모두 없는 구성원만")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthUser user, @PathVariable int memberId) {
        commandService.remove(user, memberId);
        return ResponseEntity.noContent().build();
    }
}
