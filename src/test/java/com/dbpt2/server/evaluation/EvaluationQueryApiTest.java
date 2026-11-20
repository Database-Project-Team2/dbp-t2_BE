package com.dbpt2.server.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvaluationQueryApiTest extends IntegrationTest {

    Response search(String loginId, String query) {
        return getAs("/api/v1/evaluations?size=100" + query, loginId);
    }

    @Test
    void 경영진은_전체_평가와_의견을_본다() {
        Response r = search("demo01", "").assertNoSecrets();
        assertThat(r.json().at("/page/total_elements").asLong()).isEqualTo(34);
        JsonNode first = r.data().get(0);
        assertThat(first.has("work_performance_comment")).isTrue();
        assertThat(first.get("comments_visible").asBoolean()).isTrue();
    }

    @Test
    void 일반_직원은_소속_프로젝트_점수만_보고_의견_필드는_응답에_없다() {
        Response r = search("demo03", "").assertNoSecrets();
        assertThat(r.json().at("/page/total_elements").asLong()).isEqualTo(23);
        assertThat(r.body()).doesNotContain("work_performance_comment", "communication_comment", "맡은 업무를 수행하고");
        List<Integer> projects = new ArrayList<>();
        r.data().forEach(n -> projects.add(n.get("project_id").asInt()));
        assertThat(projects).containsOnly(1, 4);
    }

    @Test
    void PM은_담당_프로젝트의_의견을_본다() {
        Response r = search("demo05", "");
        assertThat(r.json().at("/page/total_elements").asLong()).isEqualTo(20);
        r.data().forEach(n -> assertThat(n.has("work_performance_comment")).isTrue());
    }

    @Test
    void 다른_프로젝트에서_PM이어도_일반_구성원인_프로젝트의_의견은_빠진다() {
        jdbc.update("INSERT INTO PROJECT_MEMBER (project_id, employee_id, project_role, assignment_start_date, assignment_end_date)"
                + " VALUES (1, 5, 'TESTER', '2026-01-05', '2026-03-31')");
        Response p1 = search("demo05", "&project_id=1");
        assertThat(p1.json().at("/page/total_elements").asLong()).isEqualTo(14);
        assertThat(p1.body()).doesNotContain("work_performance_comment");
        assertThat(search("demo05", "&project_id=2").data().get(0).has("work_performance_comment")).isTrue();
    }

    @Test
    void 검색_조건() {
        assertThat(search("demo01", "&evaluation_type=CLIENT").json().at("/page/total_elements").asLong()).isEqualTo(7);
        assertThat(search("demo01", "&project_id=2").json().at("/page/total_elements").asLong()).isEqualTo(11);
        assertThat(search("demo01", "&target_employee_name=이서연").json().at("/page/total_elements").asLong()).isEqualTo(3);
        assertThat(search("demo01", "&target_employee_id=2").json().at("/page/total_elements").asLong()).isEqualTo(3);
        assertThat(search("demo01", "&from=2026-07-01&to=2026-07-31").json().at("/page/total_elements").asLong()).isEqualTo(11);
        assertThat(search("demo01", "&from=2026-07-31&to=2026-07-01").status()).isEqualTo(400);
        assertThat(search("demo01", "&evaluation_type=BOSS").status()).isEqualTo(400);
        assertThat(search(null, "").status()).isEqualTo(401);
        Response first = search("demo01", "&evaluation_type=CLIENT&project_id=1");
        assertThat(first.data().get(0).at("/evaluator/client_contact").asBoolean()).isTrue();
        assertThat(first.data().get(0).at("/evaluator/name").asText()).isEqualTo("김담당");
        assertThat(first.data().get(0).get("evaluator_member_id").isNull()).isTrue();
    }

    @Test
    void 상세도_같은_범위와_의견_권한() {
        assertThat(getAs("/api/v1/evaluations/15", "demo03").status()).isEqualTo(404);
        Response score = getAs("/api/v1/evaluations/1", "demo03");
        assertThat(score.status()).isEqualTo(200);
        assertThat(score.body()).doesNotContain("work_performance_comment");
        assertThat(getAs("/api/v1/evaluations/1", "demo01").data().get("work_performance_comment").asText()).isNotBlank();
        assertThat(getAs("/api/v1/evaluations/999", "demo01").status()).isEqualTo(404);
    }

    @Test
    void CSV는_BOM과_같은_행_의견_권한을_적용한다() {
        Response manager = getAs("/api/v1/evaluations/export.csv", "demo01");
        assertThat(manager.status()).isEqualTo(200);
        assertThat(manager.contentType()).startsWith("text/csv");
        assertThat(manager.contentDisposition()).contains("attachment").contains("filename*=UTF-8''");
        assertThat(manager.body()).startsWith("﻿").contains("업무수행 의견");
        assertThat(manager.body().split("\r\n")).hasSize(35);

        Response employee = getAs("/api/v1/evaluations/export.csv", "demo03");
        assertThat(employee.body()).doesNotContain("업무수행 의견", "맡은 업무를 수행하고").doesNotContain("연구 데이터 관리 시스템");
        assertThat(employee.body().split("\r\n")).hasSize(24);
        assertThat(getAs("/api/v1/evaluations/export.csv?project_id=2", "demo05").body()).contains("업무수행 의견");
        assertThat(getAs("/api/v1/evaluations/export.csv", null).status()).isEqualTo(401);
    }

    @Test
    void 평가_가능한_프로젝트와_허용_유형() {
        Response r = getAs("/api/v1/evaluations/eligible-projects", "demo03");
        assertThat(r.data()).hasSize(2);
        for (JsonNode p : r.data()) {
            assertThat(p.get("allowed_types").toString()).isEqualTo("[\"PEER\"]");
            assertThat(p.get("remaining_count").asLong()).isEqualTo(3);
        }
        Response pm = getAs("/api/v1/evaluations/eligible-projects", "demo05");
        assertThat(pm.data().get(0).get("allowed_types").toString()).isEqualTo("[\"PEER\",\"PM\",\"CLIENT\"]");
        assertThat(getAs("/api/v1/evaluations/eligible-projects", "demo02").data()).as("P3 진행 중, P1만").hasSize(1);
    }

    @Test
    void 평가_대상_선택() {
        Response peer = getAs("/api/v1/projects/4/evaluation-options?evaluation_type=PEER", "demo03");
        assertThat(peer.status()).isEqualTo(200);
        List<Integer> targets = new ArrayList<>();
        peer.data().get("available_targets").forEach(n -> targets.add(n.get("member_id").asInt()));
        assertThat(targets).containsExactlyInAnyOrder(16, 19, 20);
        assertThat(peer.data().get("can_submit").asBoolean()).isTrue();
        assertThat(peer.data().at("/registered_by/member_id").asInt()).isEqualTo(17);
        assertThat(getAs("/api/v1/projects/4/evaluation-options?evaluation_type=PM", "demo03").status()).isEqualTo(403);
        assertThat(getAs("/api/v1/projects/4/evaluation-options?evaluation_type=PEER", "demo08").status()).isEqualTo(404);
        Response client = getAs("/api/v1/projects/4/evaluation-options?evaluation_type=CLIENT", "demo05");
        assertThat(client.data().get("can_submit").asBoolean()).isFalse();
        assertThat(client.data().get("blocked_reason").asText()).contains("의뢰일");
        assertThat(client.data().at("/evaluator/name").asText()).isEqualTo("김담당");
        Response open = getAs("/api/v1/projects/3/evaluation-options?evaluation_type=PEER", "demo02");
        assertThat(open.data().get("can_submit").asBoolean()).isFalse();
        assertThat(getAs("/api/v1/projects/4/evaluation-options", "demo03").status()).isEqualTo(400);
    }
}
