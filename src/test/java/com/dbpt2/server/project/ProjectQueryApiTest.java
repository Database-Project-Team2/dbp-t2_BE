package com.dbpt2.server.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectQueryApiTest extends IntegrationTest {

    private List<Integer> ids(Response r) {
        List<Integer> out = new ArrayList<>();
        r.data().forEach(n -> out.add(n.get("project_id").asInt()));
        return out;
    }

    @Test
    void 경영진은_전체_프로젝트와_상태별_건수를_본다() {
        Response r = getAs("/api/v1/projects", "demo01").assertNoSecrets();
        assertThat(r.status()).isEqualTo(200);
        assertThat(ids(r)).containsExactly(1, 2, 3, 4);
        assertThat(r.json().at("/page/total_elements").asLong()).isEqualTo(4);
        assertThat(r.json().at("/page/size").asInt()).isEqualTo(20);
        assertThat(r.json().at("/status_counts/IN_PROGRESS").asLong()).isEqualTo(1);
        assertThat(r.json().at("/status_counts/CLOSED").asLong()).isEqualTo(3);
        JsonNode p3 = r.data().get(2);
        assertThat(p3.get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(p3.at("/client/client_name").asText()).isEqualTo("가상 새봄교육");
        assertThat(p3.get("member_count").asLong()).isEqualTo(5);
        assertThat(p3.get("current_member_count").asLong()).isEqualTo(3);
        assertThat(r.data().get(1).get("workflow_status").asText()).isEqualTo("COLLECTING");
    }

    @Test
    void 이름_발주처_상태_기준일_기간으로_검색한다() {
        assertThat(ids(getAs("/api/v1/projects?project_name=유통", "demo01"))).containsExactly(1, 4);
        assertThat(ids(getAs("/api/v1/projects?client_name=미래", "demo01"))).containsExactly(2);
        assertThat(ids(getAs("/api/v1/projects?project_name=유통&client_name=한빛", "demo01"))).containsExactly(1, 4);
        assertThat(ids(getAs("/api/v1/projects?status=CLOSED", "demo01"))).containsExactly(1, 2, 4);
        assertThat(ids(getAs("/api/v1/projects?as_of=2026-05-01", "demo01"))).containsExactly(2);
        assertThat(ids(getAs("/api/v1/projects?as_of=2026-03-31", "demo01"))).as("종료일 당일 포함").containsExactly(1);
        assertThat(ids(getAs("/api/v1/projects?from=2026-06-01&to=2026-07-10", "demo01"))).containsExactly(2, 4);
        assertThat(ids(getAs("/api/v1/projects?from=2026-10-01", "demo01"))).as("종료일 없음은 열린 구간").containsExactly(3);
        assertThat(ids(getAs("/api/v1/projects?status=IN_PROGRESS&as_of=2026-05-01", "demo01"))).containsExactly(2);
        assertThat(ids(getAs("/api/v1/projects?project_name=%25", "demo01"))).as("%는 글자 그대로").isEmpty();
    }

    @Test
    void 잘못된_검색_조건은_400() {
        assertThat(getAs("/api/v1/projects?from=2026-07-01&to=2026-06-01", "demo01").status()).isEqualTo(400);
        assertThat(getAs("/api/v1/projects?status=DONE", "demo01").status()).isEqualTo(400);
        assertThat(getAs("/api/v1/projects?size=101", "demo01").status()).isEqualTo(400);
        assertThat(getAs("/api/v1/projects?size=0", "demo01").status()).isEqualTo(400);
        assertThat(getAs("/api/v1/projects?page=-1", "demo01").status()).isEqualTo(400);
    }

    @Test
    void 페이지_정보를_돌려준다() {
        Response r = getAs("/api/v1/projects?size=3&page=1", "demo01");
        assertThat(ids(r)).containsExactly(4);
        assertThat(r.json().at("/page/total_pages").asInt()).isEqualTo(2);
        assertThat(r.json().at("/page/has_next").asBoolean()).isFalse();
        assertThat(getAs("/api/v1/projects?size=3", "demo01").json().at("/page/has_next").asBoolean()).isTrue();
    }

    @Test
    void 직원은_본인_소속_프로젝트만_본다_투입_전_포함() {
        assertThat(ids(getAs("/api/v1/projects", "demo03"))).containsExactly(1, 4);
        jdbc.update("INSERT INTO PROJECT_MEMBER (project_id, employee_id, project_role) VALUES (3, 4, 'TESTER')");
        assertThat(ids(getAs("/api/v1/projects", "demo04"))).containsExactly(1, 3, 4);
        assertThat(getAs("/api/v1/projects", "demo03").json().at("/status_counts/CLOSED").asLong()).isEqualTo(2);
    }

    @Test
    void 상세는_경영진과_구성원만_범위_밖은_404() {
        assertThat(getAs("/api/v1/projects/3", "demo03").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/projects/999", "demo01").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/projects/1", null).status()).isEqualTo(401);
        Response r = getAs("/api/v1/projects/1", "demo03").assertNoSecrets();
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.data().at("/my_membership/project_role").asText()).isEqualTo("PROGRAMMER");
        assertThat(r.data().at("/my_membership/actual_participant").asBoolean()).isTrue();
        assertThat(r.data().at("/permissions/can_manage").asBoolean()).isFalse();
        assertThat(r.data().at("/permissions/can_record_customer_evaluation").asBoolean()).isFalse();
        assertThat(r.data().at("/permissions/can_view_evaluation_comments").asBoolean()).isFalse();
        assertThat(r.data().at("/permissions/can_evaluate").asBoolean()).isTrue();
        assertThat(r.data().at("/client/contact_email").asText()).isEqualTo("hanbit@example.com");
    }

    @Test
    void PM_상세에는_고객평가_권한과_진행_집계가_있다() {
        JsonNode d = getAs("/api/v1/projects/2", "demo05").data();
        assertThat(d.at("/permissions/can_record_customer_evaluation").asBoolean()).isTrue();
        assertThat(d.at("/permissions/can_view_evaluation_comments").asBoolean()).isTrue();
        assertThat(d.at("/customer_evaluation_progress/participant_count").asLong()).isEqualTo(5);
        assertThat(d.at("/customer_evaluation_progress/received_count").asLong()).isEqualTo(2);
        assertThat(d.get("workflow_status").asText()).isEqualTo("COLLECTING");
    }

    @Test
    void 경영진_상세는_관리_권한이_있고_본인_역할은_없을_수_있다() {
        JsonNode d = getAs("/api/v1/projects/2", "demo01").data();
        assertThat(d.at("/permissions/can_manage").asBoolean()).isTrue();
        assertThat(d.get("my_membership").isNull()).isTrue();
    }

    @Test
    void 구성원_목록은_직원_요약과_투입_상태를_준다() {
        Response r = getAs("/api/v1/projects/3/members", "demo02").assertNoSecrets();
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.data()).hasSize(5);
        assertThat(r.data().get(0).at("/employee/employee_name").asText()).isEqualTo("김민준");
        assertThat(r.data().get(2).get("assignment_status").asText()).isEqualTo("ENDED");
        assertThat(r.data().get(4).get("assignment_status").asText()).isEqualTo("NOT_ASSIGNED");
        assertThat(r.body()).doesNotContain("login_id", "resident_no_masked", "education");
        assertThat(getAs("/api/v1/projects/3/members?as_of=2026-09-10", "demo02").data().get(2).get("assignment_status").asText())
                .isEqualTo("ASSIGNED");
    }

    @Test
    void 구성원_목록도_범위_밖은_404_토큰_없으면_401() {
        assertThat(getAs("/api/v1/projects/2/members", "demo03").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/projects/999/members", "demo01").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/projects/2/members", null).status()).isEqualTo(401);
    }
}
