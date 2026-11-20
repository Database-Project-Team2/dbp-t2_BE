package com.dbpt2.server.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectCommandApiTest extends IntegrationTest {

    static Map<String, Object> project(Map<String, Object> overrides) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "project_name", "새봄 학습관리 시스템", "client_id", 3, "actual_start_date", TODAY, "planned_end_date", "2026-12-31"));
        body.putAll(overrides);
        return body;
    }

    int create() {
        return postAs("/api/v1/projects", "demo01", project(Map.of())).data().get("project_id").asInt();
    }

    @Test
    void 경영진은_프로젝트를_등록하고_상세를_201로_받는다() {
        Response r = postAs("/api/v1/projects", "demo01", project(Map.of("project_name", "  새봄 학습관리 시스템  ")));
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.data().get("project_name").asText()).isEqualTo("새봄 학습관리 시스템");
        assertThat(r.data().get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(r.data().at("/client/client_name").asText()).isEqualTo("가상 새봄교육");
        assertThat(r.data().get("member_count").asLong()).isZero();
    }

    @Test
    void 일반_직원과_PM은_프로젝트를_등록_수정_종료할_수_없다() {
        assertThat(postAs("/api/v1/projects", "demo03", project(Map.of())).status()).isEqualTo(403);
        assertThat(postAs("/api/v1/projects", "demo05", project(Map.of())).status()).isEqualTo(403);
        assertThat(postAs("/api/v1/projects", "demo05", Map.of()).status()).as("본문 검사보다 권한 검사가 먼저").isEqualTo(403);
        assertThat(patchAs("/api/v1/projects/3", "demo02", Map.of("project_name", "x")).status()).isEqualTo(403);
        assertThat(postAs("/api/v1/projects/3/close", "demo02", Map.of("actual_end_date", TODAY)).status()).isEqualTo(403);
        assertThat(postAs("/api/v1/projects", null, project(Map.of())).status()).isEqualTo(401);
        assertThat(getAs("/api/v1/projects/3/client-options", "demo02").status()).isEqualTo(403);
    }

    @Test
    void 등록_입력_위반은_400_404_422() {
        assertThat(postAs("/api/v1/projects", "demo01", project(Map.of("project_name", " "))).status()).isEqualTo(400);
        assertThat(postAs("/api/v1/projects", "demo01", project(Map.of("project_name", "가".repeat(201)))).status()).isEqualTo(400);
        Map<String, Object> missing = project(Map.of());
        missing.remove("planned_end_date");
        assertThat(postAs("/api/v1/projects", "demo01", missing).status()).isEqualTo(400);
        assertThat(postAs("/api/v1/projects", "demo01", project(Map.of("actual_end_date", TODAY))).status()).as("종료일은 종료 API로만").isEqualTo(400);
        assertThat(postAs("/api/v1/projects", "demo01", project(Map.of("client_id", 999))).status()).isEqualTo(404);
        Response future = postAs("/api/v1/projects", "demo01", project(Map.of("actual_start_date", "2026-11-21", "planned_end_date", "2026-12-31")));
        assertThat(future.status()).isEqualTo(422);
        assertThat(future.json().at("/error/field_errors/0/field").asText()).isEqualTo("actual_start_date");
        assertThat(postAs("/api/v1/projects", "demo01", project(Map.of("planned_end_date", "2026-11-19"))).status()).isEqualTo(422);
    }

    @Test
    void 보낸_필드만_수정한다() {
        Response r = patchAs("/api/v1/projects/3", "demo01", Map.of("planned_end_date", "2026-12-15", "client_id", 1));
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.data().get("planned_end_date").asText()).isEqualTo("2026-12-15");
        assertThat(r.data().get("project_name").asText()).isEqualTo("교육 콘텐츠 관리 시스템");
        assertThat(r.data().get("client_id").asInt()).isEqualTo(1);
    }

    @Test
    void 수정_위반() {
        Map<String, Object> nullName = new HashMap<>();
        nullName.put("project_name", null);
        assertThat(patchAs("/api/v1/projects/3", "demo01", nullName).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/projects/3", "demo01", Map.of("actual_end_date", TODAY)).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/projects/3", "demo01", Map.of("actual_start_date", "2026-13-01")).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/projects/999", "demo01", Map.of("project_name", "x")).status()).isEqualTo(404);
        Response closed = patchAs("/api/v1/projects/1", "demo01", Map.of("project_name", "x"));
        assertThat(closed.status()).isEqualTo(409);
        assertThat(closed.errorCode()).isEqualTo("PROJECT_CLOSED");
        assertThat(patchAs("/api/v1/projects/3", "demo01", Map.of("planned_end_date", "2026-08-01")).status()).isEqualTo(422);
    }

    @Test
    void 착수일을_기존_구성원_투입일보다_늦추면_422() {
        Response r = patchAs("/api/v1/projects/3", "demo01", Map.of("actual_start_date", "2026-08-04"));
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.json().at("/error/field_errors/0/field").asText()).isEqualTo("actual_start_date");
        assertThat(patchAs("/api/v1/projects/3", "demo01", Map.of("actual_start_date", "2026-08-01")).status()).as("앞당기기는 가능").isEqualTo(200);
    }

    @Test
    void 종료하면_투입_중_구성원만_같은_날_종료되고_투입_전은_유지된다() {
        Response r = postAs("/api/v1/projects/3/close", "demo01", Map.of("actual_end_date", TODAY));
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.data().at("/project/status").asText()).isEqualTo("CLOSED");
        assertThat(r.data().get("closed_member_ids")).hasSize(3);
        assertThat(jdbc.queryForList("SELECT member_id FROM PROJECT_MEMBER WHERE project_id = 3 AND assignment_end_date = ?", Integer.class, TODAY))
                .containsExactlyInAnyOrder(11, 12, 14);
        assertThat(jdbc.queryForObject("SELECT assignment_end_date FROM PROJECT_MEMBER WHERE member_id = 13", String.class)).isEqualTo("2026-09-15");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PROJECT_MEMBER WHERE member_id = 15 AND assignment_start_date IS NULL AND assignment_end_date IS NULL", Integer.class)).isEqualTo(1);
    }

    @Test
    void 종료_위반() {
        assertThat(postAs("/api/v1/projects/1/close", "demo01", Map.of("actual_end_date", TODAY)).status()).isEqualTo(409);
        assertThat(postAs("/api/v1/projects/3/close", "demo01", Map.of("actual_end_date", "2026-11-21")).status()).isEqualTo(422);
        assertThat(postAs("/api/v1/projects/3/close", "demo01", Map.of("actual_end_date", "2026-08-01")).status()).isEqualTo(422);
        Response beforeMemberEnd = postAs("/api/v1/projects/3/close", "demo01", Map.of("actual_end_date", "2026-09-10"));
        assertThat(beforeMemberEnd.status()).as("구성원 13의 종료일(9/15)보다 이른 종료").isEqualTo(422);
        assertThat(postAs("/api/v1/projects/3/close", "demo01", Map.of()).status()).isEqualTo(400);
        assertThat(postAs("/api/v1/projects/999/close", "demo01", Map.of("actual_end_date", TODAY)).status()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PROJECT WHERE project_id = 3 AND actual_end_date IS NULL", Integer.class)).isEqualTo(1);
    }

    @Test
    void 수정용_발주처_목록() {
        Response r = getAs("/api/v1/projects/3/client-options?client_name=한빛", "demo01");
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.data()).hasSize(1);
        assertThat(r.data().get(0).get("client_name").asText()).isEqualTo("가상 한빛유통");
        assertThat(getAs("/api/v1/projects/3/client-options", "demo01").data()).hasSize(3);
        assertThat(getAs("/api/v1/projects/999/client-options", "demo01").status()).isEqualTo(404);
    }
}
