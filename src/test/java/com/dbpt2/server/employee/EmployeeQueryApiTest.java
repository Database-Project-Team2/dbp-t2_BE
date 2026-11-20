package com.dbpt2.server.employee;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmployeeQueryApiTest extends IntegrationTest {

    private List<String> logins(Response r) {
        List<String> out = new ArrayList<>();
        r.data().forEach(n -> out.add(n.get("login_id").asText()));
        return out;
    }

    @Test
    void 직원_검색은_경영진_전용() {
        assertThat(getAs("/api/v1/employees", "demo03").status()).isEqualTo(403);
        assertThat(getAs("/api/v1/employees", "demo05").status()).as("PM도 일반 직원").isEqualTo(403);
        assertThat(getAs("/api/v1/employees", null).status()).isEqualTo(401);
    }

    @Test
    void 경영진은_전체_직원과_현재_투입_수를_본다() {
        Response r = getAs("/api/v1/employees", "demo01").assertNoSecrets();
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.data()).hasSize(11);
        assertThat(r.data().get(0).get("employee_no").asText()).isEqualTo("20240001");
        assertThat(r.data().get(0).get("current_assignment_count").asLong()).isEqualTo(1);
        assertThat(r.data().get(2).get("current_assignment_count").asLong()).isZero();
        assertThat(r.data().get(0).get("resident_no_masked").asText()).isEqualTo("900315-*******");
    }

    @Test
    void 기술은_여러_개_AND_부분_일치() {
        assertThat(logins(getAs("/api/v1/employees?skill=Java&skill=SQL", "demo01"))).hasSize(9).doesNotContain("demo09");
        assertThat(logins(getAs("/api/v1/employees?skill=java", "demo01"))).as("대소문자 무시").hasSize(9);
        assertThat(logins(getAs("/api/v1/employees?skill=Java&skill=Python", "demo01"))).isEmpty();
        assertThat(logins(getAs("/api/v1/employees?skill=협업", "demo01"))).hasSize(9);
    }

    @Test
    void 이름_사번_직군_상태_투입_경력으로_검색한다() {
        assertThat(logins(getAs("/api/v1/employees?employee_name=김", "demo01"))).containsExactly("demo01");
        assertThat(logins(getAs("/api/v1/employees?employee_no=2025", "demo01"))).hasSize(6);
        assertThat(logins(getAs("/api/v1/employees?job_category=MARKETING", "demo01"))).containsExactly("demo08", "demo11");
        assertThat(logins(getAs("/api/v1/employees?employment_status=ON_LEAVE", "demo01"))).containsExactly("demo09");
        assertThat(logins(getAs("/api/v1/employees?assigned=true", "demo01"))).containsExactly("demo01", "demo02", "demo08");
        assertThat(logins(getAs("/api/v1/employees?assigned=false", "demo01"))).hasSize(8);
        assertThat(logins(getAs("/api/v1/employees?min_career_months=50", "demo01"))).containsExactly("demo09", "demo10");
        assertThat(logins(getAs("/api/v1/employees?job_category=DEVELOPER&min_career_months=40", "demo01"))).containsExactly("demo06", "demo07");
    }

    @Test
    void 잘못된_검색_조건은_400() {
        assertThat(getAs("/api/v1/employees?job_category=CEO", "demo01").status()).isEqualTo(400);
        assertThat(getAs("/api/v1/employees?min_career_months=-1", "demo01").status()).isEqualTo(400);
        assertThat(getAs("/api/v1/employees?assigned=maybe", "demo01").status()).isEqualTo(400);
    }

    @Test
    void 직원_상세는_경영진_또는_본인() {
        Response self = getAs("/api/v1/employees/3", "demo03").assertNoSecrets();
        assertThat(self.status()).isEqualTo(200);
        assertThat(self.data().get("resident_no_masked").asText()).isEqualTo("910104-*******");
        assertThat(getAs("/api/v1/employees/2", "demo03").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/employees/2", "demo01").status()).isEqualTo(200);
        assertThat(getAs("/api/v1/employees/999", "demo01").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/employees/2", null).status()).isEqualTo(401);
    }
}
