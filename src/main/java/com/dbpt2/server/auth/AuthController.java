package com.dbpt2.server.auth;

import com.dbpt2.server.auth.AuthResponses.LoginIdCheckResponse;
import com.dbpt2.server.auth.AuthResponses.MeResponse;
import com.dbpt2.server.auth.AuthResponses.TokenResponse;
import com.dbpt2.server.common.ApiResponse;
import com.dbpt2.server.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "공개. 퇴사자·틀린 비밀번호는 401")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(authService.login(request));
    }

    @Operation(summary = "로그아웃", description = "형식상 API. 토큰을 폐기하지 않으며 프론트가 토큰을 지운다")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 로그인 정보")
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.of(authService.me(user));
    }

    @Operation(summary = "로그인 ID 중복 확인", description = "공개. 대소문자 구분 없음")
    @GetMapping("/check-login-id")
    public ApiResponse<LoginIdCheckResponse> checkLoginId(@RequestParam(name = "login_id") String loginId) {
        return ApiResponse.of(authService.checkLoginId(loginId));
    }
}
