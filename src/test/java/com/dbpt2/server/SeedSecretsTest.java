package com.dbpt2.server;

import com.dbpt2.server.support.IntegrationTest;
import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.crypto.ResidentNoCipher;
import com.dbpt2.server.crypto.ResidentNumberRule;
import com.dbpt2.server.employee.Employee;
import com.dbpt2.server.employee.EmployeeRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 시드의 비밀번호 해시·주민번호 암호문이 서버 설정과 맞는지 확인한다. */
class SeedSecretsTest extends IntegrationTest {

    @Autowired EmployeeRepository employeeRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ResidentNoCipher cipher;

    @Test
    void 시드_비밀번호는_모두_demo1234() {
        List<Employee> employees = employeeRepository.findAll();
        assertThat(employees).hasSize(10)
                .allSatisfy(e -> assertThat(passwordEncoder.matches("demo1234", e.getPasswordHash())).isTrue());
    }

    @Test
    void 시드_주민번호는_서버_키로_복호화되고_형식_검사를_통과한다() {
        LocalDate today = LocalDate.of(2026, 11, 20);
        assertThat(employeeRepository.findAll()).allSatisfy(e -> {
            String plain = cipher.decrypt(e.getResidentNoEncrypted());
            assertThat(ResidentNumberRule.isValid(plain, today)).as(plain).isTrue();
            assertThat(cipher.mask(e.getResidentNoEncrypted())).matches("\\d{6}-\\*{7}");
        });
    }

    @Test
    void 시드_학력은_프론트_선택지_값() {
        assertThat(employeeRepository.findAll()).allSatisfy(e -> assertThat(e.getEducation()).isEqualTo("대졸(학사)"));
    }
}
