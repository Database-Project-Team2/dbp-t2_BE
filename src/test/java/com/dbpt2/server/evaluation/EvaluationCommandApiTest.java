package com.dbpt2.server.evaluation;

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

/**
 * 시드: P1 종료·보고 완료, P2 종료·의뢰(07-04)·고객평가 2/5, P3 진행 중, P4 종료·의뢰 전.
 * demo03 박지훈 = P1 구성원 3(프로그래머)·P4 구성원 17(설계자), demo05 정도윤 = P2 PM(6)·P4 PM(16).
 */
class EvaluationCommandApiTest extends IntegrationTest {

    static Map<String, Object> body(int target, String type, String date) {
        return new HashMap<>(Map.of("target_member_id", target, "evaluation_type", type, "evaluated_date", date,
                "work_performance_score", 4, "work_performance_comment", "꼼꼼하게 마무리했어요.",
                "communication_score", 5, "communication_comment", "진행 상황을 잘 공유했어요."));
    }

    Response evaluate(String loginId, int target, String type) {
        return postAs("/api/v1/evaluations", loginId, body(target, type, TODAY));
    }

    Response workflow(String loginId, int projectId, String step, String date) {
        return patchAs("/api/v1/projects/" + projectId + "/customer-evaluation", loginId, Map.of("step", step, "processed_date", date));
    }

    /** 종료된 프로젝트 11: 정도윤 PM(51), 강하린 프로그래머(52), 윤서준 테스터(53), 최수빈 투입 전(54). */
    void closedFixture() {
        jdbc.update("INSERT INTO PROJECT (project_id, project_name, client_id, actual_start_date, planned_end_date, actual_end_date)"
                + " VALUES (11, '평가 검증', 3, '2026-11-01', '2026-11-10', '2026-11-10')");
        jdbc.update("INSERT INTO PROJECT_MEMBER (member_id, project_id, employee_id, project_role, assignment_start_date, assignment_end_date) VALUES"
                + " (51, 11, 5, 'PM', '2026-11-01', '2026-11-10'), (52, 11, 6, 'PROGRAMMER', '2026-11-01', '2026-11-10'),"
                + " (53, 11, 7, 'TESTER', '2026-11-02', '2026-11-10'), (54, 11, 4, 'TESTER', NULL, NULL)");
    }

    @Test
    void 동료평가는_실제_참여자가_작성하고_평가자는_토큰으로_정해진다() {
        Response r = evaluate("demo03", 16, "PEER");
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.data().get("project_id").asInt()).isEqualTo(4);
        int id = r.data().get("evaluation_id").asInt();
        Map<String, Object> row = jdbc.queryForMap("SELECT evaluator_member_id, registered_by_member_id FROM EVALUATION WHERE evaluation_id = ?", id);
        assertThat(row.get("evaluator_member_id")).isEqualTo(17);
        assertThat(row.get("registered_by_member_id")).isEqualTo(17);
    }

    @Test
    void 평가자나_등록자를_보내면_400() {
        Map<String, Object> b = body(16, "PEER", TODAY);
        b.put("evaluator_member_id", 18);
        assertThat(postAs("/api/v1/evaluations", "demo03", b).status()).isEqualTo(400);
        Map<String, Object> c = body(16, "PEER", TODAY);
        c.put("registered_by_member_id", 18);
        assertThat(postAs("/api/v1/evaluations", "demo03", c).status()).isEqualTo(400);
    }

    @Test
    void 같은_대상_유형_평가자는_한_번만_409() {
        Response dup = evaluate("demo03", 18, "PEER");
        assertThat(dup.status()).isEqualTo(409);
        assertThat(dup.errorCode()).isEqualTo("EVALUATION_DUPLICATED");
    }

    @Test
    void 자기평가는_422() {
        assertThat(evaluate("demo03", 17, "PEER").status()).isEqualTo(422);
    }

    @Test
    void 진행_중인_프로젝트는_평가할_수_없다_422() {
        Response r = evaluate("demo02", 11, "PEER");
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.json().at("/error/message").asText()).contains("종료된 후");
    }

    @Test
    void PM이_아니면_PM평가와_고객평가는_403() {
        assertThat(evaluate("demo03", 18, "PM").status()).isEqualTo(403);
        workflow("demo05", 4, "REQUEST", TODAY);
        assertThat(evaluate("demo03", 18, "CLIENT").status()).isEqualTo(403);
    }

    @Test
    void 소속이_아닌_프로젝트의_구성원은_대상으로_찾을_수_없다_404() {
        assertThat(evaluate("demo08", 16, "PEER").status()).isEqualTo(404);
        assertThat(evaluate("demo03", 999, "PEER").status()).isEqualTo(404);
    }

    @Test
    void PM평가는_PM을_제외한_실제_참여자에게() {
        closedFixture();
        assertThat(evaluate("demo05", 52, "PM").status()).isEqualTo(201);
        assertThat(evaluate("demo05", 51, "PM").status()).as("PM 자신").isEqualTo(422);
        assertThat(evaluate("demo05", 54, "PM").status()).as("투입 전 구성원").isEqualTo(422);
        assertThat(evaluate("demo05", 52, "PM").status()).isEqualTo(409);
    }

    @Test
    void 투입_전_구성원은_평가할_수_없다_403() {
        closedFixture();
        assertThat(evaluate("demo04", 52, "PEER").status()).isEqualTo(403);
    }

    @Test
    void 평가일과_점수_의견_검사() {
        closedFixture();
        assertThat(postAs("/api/v1/evaluations", "demo06", body(53, "PEER", "2026-11-09")).status()).as("종료일 이전").isEqualTo(422);
        assertThat(postAs("/api/v1/evaluations", "demo06", body(53, "PEER", "2026-11-21")).status()).as("오늘 이후").isEqualTo(422);
        Map<String, Object> zero = body(53, "PEER", TODAY);
        zero.put("work_performance_score", 0);
        assertThat(postAs("/api/v1/evaluations", "demo06", zero).status()).isEqualTo(400);
        Map<String, Object> six = body(53, "PEER", TODAY);
        six.put("communication_score", 6);
        assertThat(postAs("/api/v1/evaluations", "demo06", six).status()).isEqualTo(400);
        Map<String, Object> blank = body(53, "PEER", TODAY);
        blank.put("communication_comment", "   ");
        assertThat(postAs("/api/v1/evaluations", "demo06", blank).status()).isEqualTo(400);
        Map<String, Object> missing = body(53, "PEER", TODAY);
        missing.remove("work_performance_comment");
        assertThat(postAs("/api/v1/evaluations", "demo06", missing).status()).isEqualTo(400);
        Map<String, Object> decimal = body(53, "PEER", TODAY);
        decimal.put("work_performance_score", 3.5);
        assertThat(postAs("/api/v1/evaluations", "demo06", decimal).status()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EVALUATION", Integer.class)).isEqualTo(34);
    }

    @Test
    void 고객평가는_의뢰_후_PM이_등록하고_대상당_한_번() {
        assertThat(evaluate("demo05", 17, "CLIENT").status()).as("의뢰 전").isEqualTo(409);
        assertThat(workflow("demo05", 4, "REQUEST", "2026-11-18").status()).isEqualTo(200);
        assertThat(postAs("/api/v1/evaluations", "demo05", body(17, "CLIENT", "2026-11-17")).status()).as("의뢰일 이전").isEqualTo(422);
        Response r = evaluate("demo05", 17, "CLIENT");
        assertThat(r.status()).isEqualTo(201);
        Map<String, Object> row = jdbc.queryForMap("SELECT evaluator_member_id, registered_by_member_id FROM EVALUATION WHERE evaluation_id = ?",
                r.data().get("evaluation_id").asInt());
        assertThat(row.get("evaluator_member_id")).isNull();
        assertThat(row.get("registered_by_member_id")).isEqualTo(16);
        assertThat(evaluate("demo05", 17, "CLIENT").status()).isEqualTo(409);
        assertThat(evaluate("demo05", 16, "CLIENT").status()).as("PM 자신도 고객평가 대상").isEqualTo(201);
    }

    @Test
    void 보고_완료된_프로젝트에는_고객평가를_추가할_수_없다() {
        jdbc.update("DELETE FROM EVALUATION WHERE evaluation_id = 5");
        Response r = evaluate("demo01", 5, "CLIENT");
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.errorCode()).isEqualTo("WORKFLOW_CONFLICT");
    }

    @Test
    void 같은_대상의_고객평가를_동시에_등록하면_한_건만_성공한다() throws Exception {
        token("demo05");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Response>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return evaluate("demo05", 8, "CLIENT");
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Response> f : futures) statuses.add(f.get().status());
        pool.shutdown();
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EVALUATION WHERE target_member_id = 8 AND evaluation_type = 'CLIENT'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void 고객평가_단계는_의뢰_회수_보고_순서이고_전원_등록_전에는_회수할_수_없다() {
        Response early = workflow("demo05", 2, "RECEIVE", TODAY);
        assertThat(early.status()).isEqualTo(409);
        assertThat(early.json().at("/error/message").asText()).contains("2/5");
        for (int target : new int[] {8, 9}) {
            assertThat(evaluate("demo05", target, "CLIENT").status()).isEqualTo(201);
        }
        assertThat(postAs("/api/v1/evaluations", "demo05", body(10, "CLIENT", "2026-11-19")).status()).isEqualTo(201);
        assertThat(workflow("demo05", 2, "REPORT", TODAY).status()).as("회수 전 보고").isEqualTo(409);
        assertThat(workflow("demo05", 2, "RECEIVE", "2026-11-19").status()).as("회수일 < 고객평가일").isEqualTo(422);
        Response received = workflow("demo05", 2, "RECEIVE", TODAY);
        assertThat(received.status()).isEqualTo(200);
        assertThat(received.data().get("workflow_status").asText()).isEqualTo("AWAITING_REPORT");
        assertThat(received.data().get("received_count").asLong()).isEqualTo(5);
        Response reported = workflow("demo05", 2, "REPORT", TODAY);
        assertThat(reported.data().get("workflow_status").asText()).isEqualTo("REPORTED");
        assertThat(workflow("demo05", 2, "REPORT", TODAY).status()).isEqualTo(409);
        jdbc.update("DELETE FROM EVALUATION WHERE target_member_id = 10 AND evaluation_type = 'CLIENT'");
        assertThat(evaluate("demo05", 10, "CLIENT").status()).as("보고 후 추가").isEqualTo(409);
    }

    @Test
    void 고객평가_단계_권한과_날짜_검사() {
        assertThat(workflow("demo03", 4, "REQUEST", TODAY).status()).as("일반 구성원").isEqualTo(403);
        assertThat(workflow("demo01", 4, "REQUEST", TODAY).status()).as("PM이 아닌 경영진").isEqualTo(403);
        assertThat(workflow("demo08", 4, "REQUEST", TODAY).status()).as("범위 밖").isEqualTo(404);
        assertThat(workflow(null, 4, "REQUEST", TODAY).status()).isEqualTo(401);
        assertThat(workflow("demo01", 3, "REQUEST", TODAY).status()).as("진행 중 프로젝트").isEqualTo(422);
        assertThat(workflow("demo05", 4, "RECEIVE", TODAY).status()).as("단계 건너뛰기").isEqualTo(409);
        assertThat(workflow("demo05", 4, "REQUEST", "2026-11-21").status()).isEqualTo(422);
        assertThat(workflow("demo05", 4, "REQUEST", "2026-09-29").status()).isEqualTo(422);
        assertThat(workflow("demo05", 4, "SEND", TODAY).status()).isEqualTo(400);
        assertThat(workflow("demo05", 999, "REQUEST", TODAY).status()).isEqualTo(404);
    }
}
