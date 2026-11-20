package com.dbpt2.server.auth;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.auth.AuthResponses.LoginIdCheckResponse;
import com.dbpt2.server.auth.AuthResponses.MeResponse;
import com.dbpt2.server.auth.AuthResponses.TokenResponse;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.ErrorCode;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.crypto.ResidentNoCipher;
import com.dbpt2.server.employee.Employee;
import com.dbpt2.server.employee.EmployeeOptions;
import com.dbpt2.server.employee.EmployeeRepository;
import com.dbpt2.server.employee.EmployeeResponse;
import com.dbpt2.server.employee.EmploymentStatus;
import com.dbpt2.server.security.AuthUser;
import com.dbpt2.server.security.JwtService;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    /** 없는 ID에도 비밀번호 검사 시간을 비슷하게 맞추기 위한 더미 해시. */
    private volatile String dummyHash;

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ResidentNoCipher cipher;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    /** 없는 ID·틀린 비밀번호·퇴사자 모두 같은 401로 응답한다. */
    public TokenResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByLoginId(EmployeeOptions.normalizeLoginId(request.loginId())).orElse(null);
        boolean matches = passwordEncoder.matches(request.password(), employee == null ? dummyHash() : employee.getPasswordHash());
        if (employee == null || !matches || employee.getEmploymentStatus() == EmploymentStatus.RESIGNED) {
            throw new ApiException(ErrorCode.LOGIN_FAILED);
        }
        return tokenFor(employee);
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
        }
        return dummyHash;
    }

    public TokenResponse tokenFor(Employee employee) {
        AuthUser user = new AuthUser(employee.getId(), employee.getSystemRole());
        return new TokenResponse(jwtService.issue(employee.getId()), EmployeeResponse.of(employee, cipher),
                accessPolicy.hasPmAssignment(user), TimeConfig.today(clock));
    }

    public MeResponse me(AuthUser user) {
        Employee employee = employeeRepository.findById(user.employeeId()).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        return new MeResponse(EmployeeResponse.of(employee, cipher), accessPolicy.hasPmAssignment(user), TimeConfig.today(clock));
    }

    public LoginIdCheckResponse checkLoginId(String loginId) {
        if (loginId == null || !EmployeeOptions.LOGIN_ID.matcher(loginId).matches()) {
            throw ApiException.invalid("login_id", "영문·숫자·마침표·밑줄·하이픈 3~100자로 입력해 주세요.");
        }
        return new LoginIdCheckResponse(loginId, !employeeRepository.existsByLoginId(EmployeeOptions.normalizeLoginId(loginId)));
    }
}
