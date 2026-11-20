package com.dbpt2.server.employee;

import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.ErrorCode;
import com.dbpt2.server.common.PatchBody;
import com.dbpt2.server.crypto.ResidentNoCipher;
import com.dbpt2.server.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 본인 정보 수정: 학력·총 경력·기술 설명만. 비밀번호는 현재 비밀번호 확인 후 변경. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResidentNoCipher cipher;

    public record PasswordRequest(String currentPassword, String newPassword) {}

    public EmployeeResponse updateProfile(AuthUser user, PatchBody patch) {
        Employee me = employeeRepository.findById(user.employeeId()).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        String education = null;
        if (patch.has("education")) {
            education = patch.text("education", false);
            if (!EmployeeOptions.EDUCATIONS.contains(education)) {
                throw ApiException.invalid("education", "최종학력을 선택해 주세요.");
            }
        }
        Integer months = null;
        if (patch.has("total_career_months")) {
            months = patch.integer("total_career_months");
            if (months < 0) {
                throw ApiException.invalid("total_career_months", "총 경력은 0 이상의 정수로 입력해 주세요.");
            }
        }
        String skill = null;
        boolean skillProvided = patch.has("skill_description");
        if (skillProvided) {
            String raw = patch.text("skill_description", true);
            skill = raw == null || raw.isBlank() ? null : raw;
            if (skill != null && skill.length() > 65535) {
                throw ApiException.invalid("skill_description", "기술 설명이 너무 길어요.");
            }
        }
        me.updateProfile(education, months, skill, skillProvided);
        return EmployeeResponse.of(me, cipher);
    }

    /** 현재 비밀번호가 틀리면 400(401은 토큰 문제로 쓰므로 로그인 화면 이동과 구분한다). */
    public void changePassword(AuthUser user, PasswordRequest r) {
        Employee me = employeeRepository.findById(user.employeeId()).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (r.currentPassword() == null || !passwordEncoder.matches(r.currentPassword(), me.getPasswordHash())) {
            throw ApiException.invalid("current_password", "현재 비밀번호가 맞지 않아요.");
        }
        EmployeeRegistrationService.validatePassword("new_password", r.newPassword());
        if (passwordEncoder.matches(r.newPassword(), me.getPasswordHash())) {
            throw ApiException.invalid("new_password", "현재와 다른 비밀번호를 입력해 주세요.");
        }
        me.changePassword(passwordEncoder.encode(r.newPassword()));
    }
}
