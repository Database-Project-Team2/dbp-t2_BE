package com.dbpt2.server.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 회원가입 입력. system_role·employee_no 등 다른 필드는 Jackson이 400으로 거부한다. */
public record RegisterRequest(
        @NotBlank(message = "직원 이름을 입력해 주세요.") @Size(max = 100, message = "직원 이름은 100자 이하로 입력해 주세요.") String employeeName,
        @NotBlank(message = "로그인 ID를 입력해 주세요.") String loginId,
        @NotNull(message = "비밀번호를 입력해 주세요.") String password,
        @NotBlank(message = "주민등록번호를 입력해 주세요.") String residentNo,
        @NotBlank(message = "최종학력을 선택해 주세요.") String education,
        @NotNull(message = "입사일을 입력해 주세요.") LocalDate hireDate,
        @NotNull(message = "총 경력을 입력해 주세요.") @PositiveOrZero(message = "총 경력은 0 이상의 개월 수로 입력해 주세요.") Integer totalCareerMonths,
        @NotNull(message = "직군을 선택해 주세요.") JobCategory jobCategory,
        String skillDescription) {}
