package com.dbpt2.server.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank(message = "로그인 ID를 입력해 주세요.") String loginId,
        @NotNull(message = "비밀번호를 입력해 주세요.") String password) {}
