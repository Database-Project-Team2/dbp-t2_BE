package com.dbpt2.server.employee;

import com.dbpt2.server.auth.AuthResponses.TokenResponse;
import com.dbpt2.server.auth.AuthService;
import com.dbpt2.server.common.ApiResponse;
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

    @Operation(summary = "회원가입", description = "공개. 모든 가입자는 EMPLOYEE, 사번은 서버 발급. system_role·employee_no를 보내면 400")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.of(authService.tokenFor(registrationService.register(request)));
    }
}
