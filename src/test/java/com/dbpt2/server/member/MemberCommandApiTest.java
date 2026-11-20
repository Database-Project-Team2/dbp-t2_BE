package com.dbpt2.server.member;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 새 프로젝트(착수 2026-11-02)를 만들어 구성원 규칙을 검사한다. 박지훈(3)·최수빈(4)·정도윤(5)은 2024 입사. */
class MemberCommandApiTest extends IntegrationTest {

    int projectId;

    @BeforeEach
    void newProject() {
        projectId = postAs("/api/v1/projects", "demo01", Map.of("project_name", "새 프로젝트", "client_id", 3,
                "actual_start_date", "2026-11-02", "planned_end_date", "2026-12-31")).data().get("project_id").asInt();
    }

    Response add(int employeeId, String role, String start, String end) {
        Map<String, Object> body = new HashMap<>(Map.of("employee_id", employeeId, "project_role", role));
        if (start != null) body.put("assignment_start_date", start);
        if (end != null) body.put("assignment_end_date", end);
        return postAs("/api/v1/projects/" + projectId + "/members", "demo01", body);
    }

    @Test
    void 구성원을_추가하면_201과_직원_요약을_준다() {
        Response r = add(3, "PM", TODAY, null).assertNoSecrets();
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.data().get("project_role").asText()).isEqualTo("PM");
        assertThat(r.data().get("assignment_status").asText()).isEqualTo("ASSIGNED");
        assertThat(r.data().at("/employee/employee_name").asText()).isEqualTo("박지훈");
        assertThat(add(4, "TESTER", null, null).data().get("assignment_status").asText()).isEqualTo("NOT_ASSIGNED");
    }

    @Test
    void 다른_프로젝트에_참여_중이어도_추가할_수_있다() {
        assertThat(add(2, "PL", TODAY, null).status()).as("이서연은 P3 참여 중").isEqualTo(201);
    }

    @Test
    void 같은_직원_재등록은_409() {
        add(3, "PM", TODAY, null);
        Response dup = add(3, "PROGRAMMER", null, null);
        assertThat(dup.status()).isEqualTo(409);
        assertThat(dup.errorCode()).isEqualTo("MEMBER_DUPLICATED");
    }

    @Test
    void 같은_직원을_동시에_추가하면_한_건만_성공한다() throws Exception {
        token("demo01");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Response>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return add(4, "TESTER", null, null);
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Response> f : futures) statuses.add(f.get().status());
        pool.shutdown();
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
    }

    @Test
    void 날짜_규칙_위반은_422() {
        Response beforeProject = add(3, "PM", "2026-11-01", null);
        assertThat(beforeProject.status()).isEqualTo(422);
        assertThat(beforeProject.json().at("/error/field_errors/0/field").asText()).isEqualTo("assignment_start_date");
        assertThat(add(3, "PM", "2026-11-21", null).status()).as("오늘 이후").isEqualTo(422);
        assertThat(add(3, "PM", TODAY, "2026-11-19").status()).as("종료 < 시작").isEqualTo(422);
        assertThat(add(3, "PM", null, TODAY).status()).as("시작 없이 종료").isEqualTo(422);
        jdbc.update("UPDATE EMPLOYEE SET hire_date = '2026-11-10' WHERE employee_id = 4");
        assertThat(add(4, "TESTER", "2026-11-05", null).status()).as("입사일 이전").isEqualTo(422);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PROJECT_MEMBER WHERE project_id = ?", Integer.class, projectId)).isZero();
    }

    @Test
    void 재직_중인_직원만_투입한다() {
        Response leave = add(9, "DESIGNER", null, null);
        assertThat(leave.status()).as("휴직 임현우").isEqualTo(422);
        assertThat(leave.json().at("/error/field_errors/0/field").asText()).isEqualTo("employee_id");
        assertThat(add(10, "TESTER", null, null).status()).as("퇴사 한지민").isEqualTo(422);
        assertThat(add(999, "TESTER", null, null).status()).isEqualTo(404);
    }

    @Test
    void 입력_형식_위반은_400() {
        assertThat(postAs("/api/v1/projects/" + projectId + "/members", "demo01", Map.of("employee_id", 3)).status()).isEqualTo(400);
        assertThat(postAs("/api/v1/projects/" + projectId + "/members", "demo01", Map.of("employee_id", 3, "project_role", "CEO")).status()).isEqualTo(400);
    }

    @Test
    void 종료된_프로젝트에는_추가할_수_없다() {
        Response r = postAs("/api/v1/projects/1/members", "demo01", Map.of("employee_id", 6, "project_role", "TESTER"));
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.errorCode()).isEqualTo("PROJECT_CLOSED");
    }

    @Test
    void PM과_일반_직원은_구성원을_관리할_수_없다() {
        add(3, "PM", TODAY, null);
        int memberId = add(4, "TESTER", null, null).data().get("member_id").asInt();
        assertThat(postAs("/api/v1/projects/" + projectId + "/members", "demo03", Map.of("employee_id", 6, "project_role", "TESTER")).status())
                .as("이 프로젝트의 PM").isEqualTo(403);
        assertThat(patchAs("/api/v1/members/" + memberId + "/assignment", "demo03", Map.of("assignment_start_date", TODAY)).status()).isEqualTo(403);
        assertThat(deleteAs("/api/v1/members/" + memberId, "demo03").status()).isEqualTo(403);
        assertThat(getAs("/api/v1/projects/" + projectId + "/member-candidates", "demo03").status()).isEqualTo(403);
        assertThat(deleteAs("/api/v1/members/" + memberId, null).status()).isEqualTo(401);
    }

    @Test
    void 후보는_재직_중이고_이_프로젝트에_없는_직원() {
        add(3, "PM", TODAY, null);
        Response r = getAs("/api/v1/projects/" + projectId + "/member-candidates?size=100", "demo01").assertNoSecrets();
        assertThat(r.status()).isEqualTo(200);
        List<String> names = new ArrayList<>();
        r.data().forEach(n -> names.add(n.get("employee_name").asText()));
        assertThat(names).contains("김민준", "이서연").doesNotContain("박지훈", "임현우", "한지민");
        assertThat(r.data().get(0).has("skill_description")).isTrue();
        assertThat(getAs("/api/v1/projects/" + projectId + "/member-candidates?employee_name=임현우", "demo01").data()).isEmpty();
        assertThat(getAs("/api/v1/projects/" + projectId + "/member-candidates?skill=java&skill=sql&employee_no=2025", "demo01").data()).hasSize(3);
        assertThat(getAs("/api/v1/projects/999/member-candidates", "demo01").status()).isEqualTo(404);
    }

    @Test
    void 투입기간을_기록하고_종료한다() {
        int memberId = add(4, "TESTER", null, null).data().get("member_id").asInt();
        Response start = patchAs("/api/v1/members/" + memberId + "/assignment", "demo01", Map.of("assignment_start_date", "2026-11-03"));
        assertThat(start.status()).isEqualTo(200);
        assertThat(start.data().get("assignment_status").asText()).isEqualTo("ASSIGNED");
        Response end = patchAs("/api/v1/members/" + memberId + "/assignment", "demo01", Map.of("assignment_end_date", "2026-11-10"));
        assertThat(end.data().get("assignment_end_date").asText()).isEqualTo("2026-11-10");
        assertThat(end.data().get("assignment_start_date").asText()).isEqualTo("2026-11-03");
        Response again = patchAs("/api/v1/members/" + memberId + "/assignment", "demo01", Map.of("assignment_end_date", "2026-11-12"));
        assertThat(again.status()).as("종료된 투입은 변경 불가").isEqualTo(409);
        assertThat(again.errorCode()).isEqualTo("ASSIGNMENT_ENDED");
    }

    @Test
    void 투입기간_수정_위반() {
        int memberId = add(3, "PM", "2026-11-03", null).data().get("member_id").asInt();
        Map<String, Object> revert = new HashMap<>();
        revert.put("assignment_start_date", null);
        assertThat(patchAs("/api/v1/members/" + memberId + "/assignment", "demo01", revert).status()).as("투입 전으로 되돌리기").isEqualTo(422);
        assertThat(patchAs("/api/v1/members/" + memberId + "/assignment", "demo01", Map.of("assignment_start_date", "2026-11-01")).status()).isEqualTo(422);
        assertThat(patchAs("/api/v1/members/" + memberId + "/assignment", "demo01", Map.of("project_role", "TESTER")).status()).as("역할 변경 불가").isEqualTo(400);
        assertThat(patchAs("/api/v1/members/999/assignment", "demo01", Map.of("assignment_start_date", TODAY)).status()).isEqualTo(404);
        assertThat(patchAs("/api/v1/members/1/assignment", "demo01", Map.of("assignment_start_date", "2026-01-06")).status())
                .as("종료된 프로젝트").isEqualTo(409);
        jdbc.update("UPDATE EMPLOYEE SET employment_status = 'ON_LEAVE' WHERE employee_id = 3");
        assertThat(patchAs("/api/v1/members/" + memberId + "/assignment", "demo01", Map.of("assignment_start_date", "2026-11-04")).status())
                .as("휴직자 기간 변경").isEqualTo(422);
    }

    @Test
    void 투입_전_구성원만_삭제한다() {
        int before = add(4, "TESTER", null, null).data().get("member_id").asInt();
        int assigned = add(3, "PM", TODAY, null).data().get("member_id").asInt();
        Response notAllowed = deleteAs("/api/v1/members/" + assigned, "demo01");
        assertThat(notAllowed.status()).isEqualTo(422);
        assertThat(deleteAs("/api/v1/members/" + before, "demo01").status()).isEqualTo(204);
        assertThat(deleteAs("/api/v1/members/" + before, "demo01").status()).isEqualTo(404);
        assertThat(deleteAs("/api/v1/members/15", "demo01").status()).as("P3 투입 전 임현우").isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE WHERE employee_id = 9", Integer.class)).isEqualTo(1);
    }

    @Test
    void 삭제와_투입_시작이_동시에_와도_실제_투입_이력은_지워지지_않는다() throws Exception {
        token("demo01");
        for (int round = 0; round < 5; round++) {
            jdbc.update("DELETE FROM PROJECT_MEMBER WHERE project_id = ? AND employee_id = 4", projectId);
            int memberId = add(4, "TESTER", null, null).data().get("member_id").asInt();
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<Response> assign = pool.submit(() -> {
                start.await();
                return patchAs("/api/v1/members/" + memberId + "/assignment", "demo01", Map.of("assignment_start_date", TODAY));
            });
            Future<Response> remove = pool.submit(() -> {
                start.await();
                return deleteAs("/api/v1/members/" + memberId, "demo01");
            });
            start.countDown();
            int assignStatus = assign.get().status();
            int removeStatus = remove.get().status();
            pool.shutdown();
            Integer remaining = jdbc.queryForObject("SELECT COUNT(*) FROM PROJECT_MEMBER WHERE member_id = ?", Integer.class, memberId);
            if (remaining == 1) {
                // 투입이 먼저: 삭제는 422, 시작일이 남아 있어야 한다.
                assertThat(assignStatus).isEqualTo(200);
                assertThat(removeStatus).isEqualTo(422);
                assertThat(jdbc.queryForObject("SELECT assignment_start_date FROM PROJECT_MEMBER WHERE member_id = ?", String.class, memberId))
                        .isEqualTo(TODAY);
            } else {
                // 삭제가 먼저: 기간 수정은 대상 없음 404.
                assertThat(removeStatus).isEqualTo(204);
                assertThat(assignStatus).isEqualTo(404);
            }
        }
    }

    @Test
    void 기간_수정과_종료가_동시에_와도_기록된_종료일을_덮어쓰지_않는다() throws Exception {
        token("demo01");
        int memberId = add(3, "PM", "2026-11-03", null).data().get("member_id").asInt();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Response> end = pool.submit(() -> {
            start.await();
            return patchAs("/api/v1/members/" + memberId + "/assignment", "demo01", Map.of("assignment_end_date", "2026-11-10"));
        });
        Future<Response> restart = pool.submit(() -> {
            start.await();
            return patchAs("/api/v1/members/" + memberId + "/assignment", "demo01", Map.of("assignment_start_date", "2026-11-04"));
        });
        start.countDown();
        List<Integer> statuses = List.of(end.get().status(), restart.get().status());
        pool.shutdown();
        String endDate = jdbc.queryForObject("SELECT assignment_end_date FROM PROJECT_MEMBER WHERE member_id = ?", String.class, memberId);
        assertThat(endDate).isEqualTo("2026-11-10");
        // 종료가 먼저면 시작일 변경은 409, 아니면 둘 다 200이고 종료일은 유지된다.
        assertThat(statuses.get(0)).isEqualTo(200);
        assertThat(statuses.get(1)).isIn(200, 409);
    }
}
