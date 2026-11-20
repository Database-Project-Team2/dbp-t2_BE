package com.dbpt2.server.auth;

import com.dbpt2.server.employee.EmployeeResponse;
import java.time.LocalDate;

public final class AuthResponses {

    private AuthResponses() {}

    /** 로그인·회원가입 응답. */
    public record TokenResponse(String accessToken, EmployeeResponse employee, boolean isPm, LocalDate serverToday) {}

    /** GET /auth/me. is_pm은 표시용이며 권한 판정에 쓰지 않는다. */
    public record MeResponse(EmployeeResponse employee, boolean isPm, LocalDate serverToday) {}

    public record LoginIdCheckResponse(String loginId, boolean available) {}
}
