package com.dbpt2.server.employee;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class RegistrationApiTest extends IntegrationTest {

    static Map<String, Object> body(Map<String, Object> overrides) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "employee_name", "신규직원",
                "login_id", "new.employee",
                "password", "TestOnly123!",
                "resident_no", "000229-3000000",
                "education", "대졸(학사)",
                "hire_date", "2026-11-02",
                "total_career_months", 0,
                "job_category", "DEVELOPER",
                "skill_description", " TypeScript, SQL "));
        body.putAll(overrides);
        return body;
    }

    private Response register(Map<String, Object> overrides) {
        return postAs("/api/v1/employees", null, body(overrides));
    }

    @Test
    void 가입하면_201과_입사연도_첫_사번_토큰을_준다() {
        Response response = register(Map.of()).assertNoSecrets();
        assertThat(response.status()).isEqualTo(201);
        assertThat(response.data().at("/employee/employee_no").asText()).isEqualTo("20260001");
        assertThat(response.data().at("/employee/system_role").asText()).isEqualTo("EMPLOYEE");
        assertThat(response.data().at("/employee/employment_status").asText()).isEqualTo("ACTIVE");
        assertThat(response.data().at("/employee/resident_no_masked").asText()).isEqualTo("000229-*******");
        assertThat(response.data().at("/employee/skill_description").asText()).isEqualTo("TypeScript, SQL");
        assertThat(response.data().get("access_token").asText()).isNotBlank();
        assertThat(response.data().get("is_pm").asBoolean()).isFalse();
        assertThat(register(Map.of("login_id", "second.one")).data().at("/employee/employee_no").asText()).isEqualTo("20260002");
        assertThat(register(Map.of("login_id", "third.one", "hire_date", "2025-12-31")).data().at("/employee/employee_no").asText())
                .isEqualTo("20250007");
    }

    @Test
    void 가입_토큰으로_바로_내_정보를_조회한다() {
        String token = register(Map.of()).data().get("access_token").asText();
        Response me = callWithToken(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/me"), token);
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.data().at("/employee/login_id").asText()).isEqualTo("new.employee");
    }

    @Test
    void 주민번호는_암호화해_저장한다() {
        register(Map.of());
        String stored = jdbc.queryForObject("SELECT resident_no_encrypted FROM EMPLOYEE WHERE login_id = 'new.employee'", String.class);
        assertThat(stored).startsWith("aesgcm:").doesNotContain("000229");
        String hash = jdbc.queryForObject("SELECT password_hash FROM EMPLOYEE WHERE login_id = 'new.employee'", String.class);
        assertThat(hash).startsWith("$2a$").doesNotContain("TestOnly123!");
    }

    @Test
    void 로그인_ID는_소문자로_저장하고_대소문자만_다른_ID는_409() {
        assertThat(register(Map.of("login_id", "New.Employee")).data().at("/employee/login_id").asText()).isEqualTo("new.employee");
        Response duplicate = register(Map.of("login_id", "NEW.EMPLOYEE"));
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.errorCode()).isEqualTo("LOGIN_ID_DUPLICATED");
        assertThat(register(Map.of("login_id", "Demo01")).status()).isEqualTo(409);
    }

    @Test
    void system_role이나_employee_no를_보내면_400() {
        Response role = register(Map.of("system_role", "MANAGEMENT"));
        assertThat(role.status()).isEqualTo(400);
        assertThat(role.json().at("/error/field_errors/0/field").asText()).isEqualTo("system_role");
        assertThat(register(Map.of("employee_no", "20260099")).status()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE", Integer.class)).isEqualTo(11);
    }

    @Test
    void 입력_형식_위반은_400() {
        assertThat(register(Map.of("login_id", "ab")).status()).isEqualTo(400);
        assertThat(register(Map.of("login_id", "bad id")).status()).isEqualTo(400);
        assertThat(register(Map.of("password", "short")).status()).isEqualTo(400);
        assertThat(register(Map.of("password", "가".repeat(30))).status()).isEqualTo(400);
        assertThat(register(Map.of("resident_no", "000230-3000000")).status()).isEqualTo(400);
        assertThat(register(Map.of("resident_no", "001301-3000000")).status()).isEqualTo(400);
        assertThat(register(Map.of("resident_no", "990101-5000000")).status()).isEqualTo(400);
        assertThat(register(Map.of("resident_no", "301231-3000000")).status()).as("미래 생년월일").isEqualTo(400);
        assertThat(register(Map.of("education", "대학교 졸업")).status()).isEqualTo(400);
        assertThat(register(Map.of("employee_name", "  ")).status()).isEqualTo(400);
        assertThat(register(Map.of("total_career_months", -1)).status()).isEqualTo(400);
        assertThat(register(Map.of("job_category", "CEO")).status()).isEqualTo(400);
        assertThat(register(Map.of("hire_date", "2026-02-30")).status()).isEqualTo(400);
        Map<String, Object> missing = body(Map.of());
        missing.remove("education");
        assertThat(postAs("/api/v1/employees", null, missing).status()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE", Integer.class)).isEqualTo(11);
    }

    @Test
    void 미래_입사일은_422() {
        Response response = register(Map.of("hire_date", "2026-11-21"));
        assertThat(response.status()).isEqualTo(422);
        assertThat(response.json().at("/error/field_errors/0/field").asText()).isEqualTo("hire_date");
    }

    @Test
    void 연도별_순번이_9999를_넘으면_422() {
        jdbc.update("""
                INSERT INTO EMPLOYEE (employee_no, employee_name, resident_no_encrypted, education, hire_date,
                  total_career_months, job_category, login_id, password_hash)
                VALUES ('20269999', '마지막', 'aesgcm:x', '고졸', '2026-01-01', 0, 'DEVELOPER', 'last.one', 'x')""");
        Response response = register(Map.of());
        assertThat(response.status()).isEqualTo(422);
        assertThat(response.errorCode()).isEqualTo("EMPLOYEE_NO_EXHAUSTED");
    }

    @Test
    void 동시에_가입한_두_명은_서로_다른_사번을_받는다() throws Exception {
        int count = 2;
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Response>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = "concurrent" + i;
            futures.add(pool.submit(() -> {
                start.await();
                return register(Map.of("login_id", id));
            }));
        }
        start.countDown();
        List<String> numbers = new ArrayList<>();
        for (Future<Response> future : futures) {
            Response response = future.get();
            assertThat(response.status()).as(response.body()).isEqualTo(201);
            numbers.add(response.data().at("/employee/employee_no").asText());
        }
        pool.shutdown();
        assertThat(numbers).containsExactlyInAnyOrder("20260001", "20260002");
    }

    @Test
    void 같은_ID로_동시에_가입하면_한_명만_성공한다() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Response>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return register(Map.of("login_id", "same.id"));
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Response> future : futures) {
            statuses.add(future.get().status());
        }
        pool.shutdown();
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
    }
}
