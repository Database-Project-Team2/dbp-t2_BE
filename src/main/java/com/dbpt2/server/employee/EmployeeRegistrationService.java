package com.dbpt2.server.employee;

import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.ErrorCode;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.crypto.ResidentNoCipher;
import com.dbpt2.server.crypto.ResidentNumberRule;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 회원가입과 사번 발급. 사번 = 입사연도 + 연도별 다음 순번(4자리).
 * 동시 가입으로 같은 사번을 고르면 UNIQUE(uq_employee_no)에 걸리고, 새 트랜잭션에서 다음 번호로 다시 시도한다.
 */
@Service
@RequiredArgsConstructor
public class EmployeeRegistrationService {

    static final int MAX_ATTEMPTS = 5;
    static final int MAX_SEQUENCE = 9999;

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResidentNoCipher cipher;
    private final Clock clock;
    private final PlatformTransactionManager transactionManager;

    public Employee register(RegisterRequest request) {
        LocalDate today = TimeConfig.today(clock);
        validate(request, today);
        String loginId = EmployeeOptions.normalizeLoginId(request.loginId());
        String passwordHash = passwordEncoder.encode(request.password());
        String residentNo = cipher.encrypt(ResidentNumberRule.normalize(request.residentNo()));
        String skill = request.skillDescription() == null || request.skillDescription().isBlank()
                ? null : request.skillDescription().trim();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        for (int attempt = 1; ; attempt++) {
            try {
                return tx.execute(status -> {
                    if (employeeRepository.existsByLoginId(loginId)) {
                        throw ApiException.field(ErrorCode.LOGIN_ID_DUPLICATED, "login_id", ErrorCode.LOGIN_ID_DUPLICATED.defaultMessage());
                    }
                    String employeeNo = nextEmployeeNo(request.hireDate());
                    return employeeRepository.saveAndFlush(Employee.register(employeeNo, request.employeeName().trim(), residentNo,
                            request.education(), request.hireDate(), request.totalCareerMonths(), skill,
                            request.jobCategory(), loginId, passwordHash));
                });
            } catch (DataIntegrityViolationException e) {
                String cause = String.valueOf(e.getMostSpecificCause().getMessage());
                if (cause.contains("uq_employee_login_id")) {
                    throw ApiException.field(ErrorCode.LOGIN_ID_DUPLICATED, "login_id", ErrorCode.LOGIN_ID_DUPLICATED.defaultMessage());
                }
                if (!cause.contains("uq_employee_no") || attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    String nextEmployeeNo(LocalDate hireDate) {
        String year = String.format("%04d", hireDate.getYear());
        int next = employeeRepository.findMaxEmployeeNoOfYear(year)
                .map(max -> Integer.parseInt(max.substring(4)) + 1)
                .orElse(1);
        if (next > MAX_SEQUENCE) {
            throw new ApiException(ErrorCode.EMPLOYEE_NO_EXHAUSTED);
        }
        return year + String.format("%04d", next);
    }

    /** 프론트 registration.ts·authValidation.ts와 같은 규칙. */
    private void validate(RegisterRequest r, LocalDate today) {
        if (!EmployeeOptions.LOGIN_ID.matcher(r.loginId()).matches()) {
            throw ApiException.invalid("login_id", "로그인 ID는 영문·숫자·마침표·밑줄·하이픈으로 3~100자 입력해 주세요.");
        }
        validatePassword("password", r.password());
        if (!ResidentNumberRule.isValid(r.residentNo(), today)) {
            throw ApiException.invalid("resident_no", "주민등록번호 입력이 잘못되었어요.");
        }
        if (!EmployeeOptions.EDUCATIONS.contains(r.education())) {
            throw ApiException.invalid("education", "최종학력을 선택해 주세요.");
        }
        if (r.hireDate().isAfter(today)) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "hire_date", "입사일은 오늘 이후일 수 없어요.");
        }
        if (r.skillDescription() != null && r.skillDescription().length() > 65535) {
            throw ApiException.invalid("skill_description", "기술 설명이 너무 길어요.");
        }
    }

    public static void validatePassword(String field, String password) {
        if (password == null || password.length() < EmployeeOptions.PASSWORD_MIN) {
            throw ApiException.invalid(field, "비밀번호를 8자 이상 입력해 주세요.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > EmployeeOptions.PASSWORD_MAX_BYTES) {
            throw ApiException.invalid(field, "비밀번호는 72바이트 이하로 입력해 주세요.");
        }
    }
}
