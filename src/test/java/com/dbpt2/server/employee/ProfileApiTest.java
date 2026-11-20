package com.dbpt2.server.employee;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfileApiTest extends IntegrationTest {

    @Test
    void 학력_총경력_기술만_보낸_필드대로_수정한다() {
        Response r = patchAs("/api/v1/employees/me", "demo03", Map.of("education", "석사", "total_career_months", 40)).assertNoSecrets();
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.data().get("education").asText()).isEqualTo("석사");
        assertThat(r.data().get("total_career_months").asInt()).isEqualTo(40);
        assertThat(r.data().get("skill_description").asText()).isEqualTo("Java, SQL 및 프로젝트 협업 경험");
        Map<String, Object> clear = new HashMap<>();
        clear.put("skill_description", null);
        assertThat(patchAs("/api/v1/employees/me", "demo03", clear).data().get("skill_description").isNull()).isTrue();
        assertThat(patchAs("/api/v1/employees/me", "demo03", Map.of("skill_description", "  Kotlin  ")).data().get("skill_description").asText())
                .isEqualTo("Kotlin");
    }

    @Test
    void 다른_필드는_바꿀_수_없다_400() {
        assertThat(patchAs("/api/v1/employees/me", "demo03", Map.of("system_role", "MANAGEMENT")).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/employees/me", "demo03", Map.of("employee_name", "다른 이름")).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/employees/me", "demo03", Map.of("hire_date", "2020-01-01")).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/employees/me", "demo03", Map.of("login_id", "other")).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/employees/me", "demo03", Map.of("education", "대학교 졸업")).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/employees/me", "demo03", Map.of("total_career_months", -1)).status()).isEqualTo(400);
        Map<String, Object> nullEducation = new HashMap<>();
        nullEducation.put("education", null);
        assertThat(patchAs("/api/v1/employees/me", "demo03", nullEducation).status()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT system_role FROM EMPLOYEE WHERE employee_id = 3", String.class)).isEqualTo("EMPLOYEE");
        assertThat(patchAs("/api/v1/employees/me", null, Map.of("education", "석사")).status()).isEqualTo(401);
    }

    @Test
    void 비밀번호는_현재_비밀번호_확인_후_변경하고_204() {
        Response r = patchAs("/api/v1/employees/me/password", "demo03", Map.of("current_password", "demo1234", "new_password", "NewPass123!"));
        assertThat(r.status()).isEqualTo(204);
        assertThat(postAs("/api/v1/auth/login", null, Map.of("login_id", "demo03", "password", "demo1234")).status()).isEqualTo(401);
        assertThat(postAs("/api/v1/auth/login", null, Map.of("login_id", "demo03", "password", "NewPass123!")).status()).isEqualTo(200);
    }

    @Test
    void 비밀번호_변경_위반() {
        Response wrong = patchAs("/api/v1/employees/me/password", "demo03", Map.of("current_password", "nope", "new_password", "NewPass123!"));
        assertThat(wrong.status()).isEqualTo(400);
        assertThat(wrong.json().at("/error/field_errors/0/field").asText()).isEqualTo("current_password");
        assertThat(patchAs("/api/v1/employees/me/password", "demo03", Map.of("current_password", "demo1234", "new_password", "short")).status())
                .isEqualTo(400);
        assertThat(patchAs("/api/v1/employees/me/password", "demo03", Map.of("current_password", "demo1234", "new_password", "demo1234")).status())
                .isEqualTo(400);
        assertThat(patchAs("/api/v1/employees/me/password", "demo03", Map.of("new_password", "NewPass123!")).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/employees/me/password", null, Map.of("current_password", "demo1234", "new_password", "NewPass123!")).status())
                .isEqualTo(401);
    }

    @Test
    void 기술_설명은_바이트_기준으로_제한한다() {
        assertThat(patchAs("/api/v1/employees/me", "demo03", Map.of("skill_description", "가".repeat(22_000))).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/employees/me", "demo03", Map.of("skill_description", "가".repeat(21_000))).status()).isEqualTo(200);
        assertThat(postAs("/api/v1/employees", null, RegistrationApiTest.body(Map.of("skill_description", "가".repeat(22_000)))).status())
                .isEqualTo(400);
    }

    @Test
    void 프로필_수정과_비밀번호_변경이_동시에_와도_새_비밀번호가_유지된다() throws Exception {
        token("demo03");
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        var password = pool.submit(() -> {
            start.await();
            return patchAs("/api/v1/employees/me/password", "demo03", Map.of("current_password", "demo1234", "new_password", "NewPass123!"));
        });
        var profile = pool.submit(() -> {
            start.await();
            return patchAs("/api/v1/employees/me", "demo03", Map.of("total_career_months", 50));
        });
        start.countDown();
        assertThat(password.get().status()).isEqualTo(204);
        assertThat(profile.get().status()).isEqualTo(200);
        pool.shutdown();
        assertThat(postAs("/api/v1/auth/login", null, Map.of("login_id", "demo03", "password", "NewPass123!")).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT total_career_months FROM EMPLOYEE WHERE employee_id = 3", Integer.class)).isEqualTo(50);
    }
}
