package com.dbpt2.server.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssignmentQueryApiTest extends IntegrationTest {

    private List<Integer> ids(Response r) {
        List<Integer> out = new ArrayList<>();
        r.data().forEach(n -> out.add(n.get("member_id").asInt()));
        return out;
    }

    private Response search(String query) {
        return getAs("/api/v1/assignments?size=100" + query, "demo01");
    }

    @Test
    void 투입_이력은_경영진_전용() {
        assertThat(getAs("/api/v1/assignments", "demo03").status()).isEqualTo(403);
        assertThat(getAs("/api/v1/assignments", "demo05").status()).isEqualTo(403);
        assertThat(getAs("/api/v1/assignments", null).status()).isEqualTo(401);
    }

    @Test
    void 전체_이력은_투입_전_포함_20건() {
        Response r = search("").assertNoSecrets();
        assertThat(r.json().at("/page/total_elements").asLong()).isEqualTo(20);
        assertThat(r.data().get(0).at("/project/project_name").asText()).isNotBlank();
        assertThat(r.data().get(0).at("/employee/employee_no").asText()).isNotBlank();
    }

    @Test
    void 기준일은_시작일과_종료일_당일을_포함한다() {
        assertThat(ids(search("&as_of=2026-05-01"))).containsExactlyInAnyOrder(6, 7, 8, 9, 10);
        assertThat(ids(search("&as_of=2026-01-05"))).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        assertThat(ids(search("&as_of=2026-03-31"))).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        assertThat(ids(search("&as_of=2026-09-15"))).contains(13);
        assertThat(ids(search("&as_of=2026-09-16"))).doesNotContain(13).doesNotContain(15);
    }

    @Test
    void 기간은_겹치는_구간이고_종료일_없음은_열린_구간() {
        assertThat(ids(search("&from=2026-09-16&to=2026-09-20"))).containsExactlyInAnyOrder(11, 12, 14, 16, 17, 18, 19, 20);
        assertThat(ids(search("&from=2026-11-01"))).containsExactlyInAnyOrder(11, 12, 14);
        assertThat(search("&from=2026-09-20&to=2026-09-01").status()).isEqualTo(400);
    }

    @Test
    void 직원_프로젝트_역할_진행_중_조건() {
        assertThat(ids(search("&project_id=3"))).containsExactlyInAnyOrder(11, 12, 13, 14, 15);
        assertThat(ids(search("&project_role=PM"))).containsExactlyInAnyOrder(1, 6, 11, 16);
        assertThat(ids(search("&employee_name=장예린"))).containsExactlyInAnyOrder(5, 9, 14);
        assertThat(ids(search("&employee_id=3"))).containsExactlyInAnyOrder(3, 17);
        assertThat(ids(search("&ongoing_projects_only=true"))).containsExactlyInAnyOrder(11, 12, 13, 14, 15);
        assertThat(ids(search("&ongoing_projects_only=true&as_of=2026-05-01"))).containsExactlyInAnyOrder(6, 7, 8, 9, 10);
        assertThat(search("&project_role=CEO").status()).isEqualTo(400);
    }

    @Test
    void 페이지_기본값은_20건() {
        Response r = getAs("/api/v1/assignments?size=5", "demo01");
        assertThat(r.data()).hasSize(5);
        assertThat(r.json().at("/page/has_next").asBoolean()).isTrue();
        assertThat(r.json().at("/page/total_pages").asInt()).isEqualTo(4);
    }
}
