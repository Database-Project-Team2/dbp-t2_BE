package com.dbpt2.server.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardApiTest extends IntegrationTest {

    private static List<String> texts(JsonNode array, String field) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.get(field).asText()));
        return out;
    }

    @Test
    void 경영진_대시보드_지표는_진행_1_참여없음_5_보고대기_2() {
        Response r = getAs("/api/v1/dashboard", "demo01").assertNoSecrets();
        assertThat(r.status()).isEqualTo(200);
        JsonNode d = r.data();
        assertThat(d.get("view").asText()).isEqualTo("MANAGEMENT");
        assertThat(d.at("/metrics/ongoing_project_count").asLong()).isEqualTo(1);
        assertThat(d.at("/metrics/total_project_count").asLong()).isEqualTo(5);
        assertThat(d.at("/metrics/employees_without_projects").asLong()).isEqualTo(5);
        assertThat(d.at("/metrics/customer_evaluation_pending").asLong()).isEqualTo(2);
        assertThat(texts(d.get("ongoing_projects"), "project_name")).containsExactly("교육 콘텐츠 관리 시스템");
        assertThat(d.at("/ongoing_projects/0/current_member_count").asLong()).isEqualTo(3);
        assertThat(d.at("/ongoing_projects/0/overdue").asBoolean()).isTrue();
        assertThat(texts(d.get("pending_customer_evaluations"), "workflow_status")).containsExactly("COLLECTING", "NOT_REQUESTED");
        assertThat(d.at("/pending_customer_evaluations/0/participant_count").asLong()).isEqualTo(5);
        assertThat(d.at("/pending_customer_evaluations/0/received_count").asLong()).isEqualTo(2);
        assertThat(d.get("current_assignments")).hasSize(3);
        assertThat(d.get("server_today").asText()).isEqualTo(TODAY);
    }

    @Test
    void 기준일을_바꾸면_당시_진행_프로젝트와_투입_현황을_보여준다() {
        JsonNode d = getAs("/api/v1/dashboard?as_of=2026-05-01", "demo01").data();
        assertThat(d.get("as_of").asText()).isEqualTo("2026-05-01");
        assertThat(texts(d.get("ongoing_projects"), "project_name")).containsExactly("연구 데이터 관리 시스템");
        assertThat(d.at("/metrics/ongoing_project_count").asLong()).isEqualTo(1);
        assertThat(d.get("current_assignments")).hasSize(5);
        // 종료일 당일은 종료로 본다(실제 종료일이 기준일 이하).
        assertThat(getAs("/api/v1/dashboard?as_of=2026-03-31", "demo01").data().at("/metrics/ongoing_project_count").asLong()).isZero();
        assertThat(getAs("/api/v1/dashboard?as_of=2026-03-30", "demo01").data().at("/metrics/ongoing_project_count").asLong()).isEqualTo(1);
    }

    @Test
    void 직원_대시보드는_본인_참여_프로젝트와_남은_평가만_보여준다() {
        Response r = getAs("/api/v1/dashboard", "demo03").assertNoSecrets();
        JsonNode d = r.data();
        assertThat(d.get("view").asText()).isEqualTo("EMPLOYEE");
        assertThat(d.get("my_projects")).isEmpty();
        assertThat(d.at("/metrics/pending_evaluation_count").asLong()).isEqualTo(6);
        assertThat(d.at("/metrics/pending_evaluation_project_count").asLong()).isEqualTo(2);
        assertThat(d.get("customer_evaluation_tasks")).isEmpty();
        assertThat(r.body()).doesNotContain("ongoing_projects", "current_assignments", "employees_without_projects");
    }

    @Test
    void 참여_중인_직원은_역할과_투입_인원을_본다() {
        JsonNode d = getAs("/api/v1/dashboard", "demo02").data();
        assertThat(texts(d.get("my_projects"), "project_name")).containsExactly("교육 콘텐츠 관리 시스템");
        assertThat(d.at("/my_projects/0/my_role").asText()).isEqualTo("PL");
        assertThat(d.at("/metrics/active_project_count").asLong()).isEqualTo(1);
        assertThat(d.at("/metrics/upcoming_project_count").asLong()).isZero();
    }

    @Test
    void PM은_담당_프로젝트의_고객평가_진행을_할_일로_본다() {
        JsonNode d = getAs("/api/v1/dashboard", "demo05").data();
        assertThat(texts(d.get("customer_evaluation_tasks"), "project_name")).containsExactly("연구 데이터 관리 시스템", "유통 분석 대시보드");
        assertThat(d.get("has_current_pm_assignment").asBoolean()).isFalse();
        assertThat(d.get("upcoming_pm_projects")).isEmpty();
    }

    @Test
    void PM_D7_안내는_예정_종료일이_오늘부터_7일_안인_참여_중_프로젝트만() {
        jdbc.update("INSERT INTO PROJECT (project_id, project_name, client_id, actual_start_date, planned_end_date) VALUES"
                + " (11, 'D-7 대상', 1, '2026-11-01', '2026-11-27'), (12, 'D-8 제외', 1, '2026-11-01', '2026-11-28'),"
                + " (13, '기한 경과 제외', 1, '2026-11-01', '2026-11-19'), (14, 'D-Day', 1, '2026-11-01', '2026-11-20'),"
                + " (15, '투입 종료 제외', 1, '2026-11-01', '2026-11-22')");
        jdbc.update("INSERT INTO PROJECT_MEMBER (project_id, employee_id, project_role, assignment_start_date, assignment_end_date) VALUES"
                + " (11, 4, 'PM', '2026-11-02', NULL), (12, 4, 'PM', NULL, NULL), (13, 4, 'PM', '2026-11-02', NULL),"
                + " (14, 4, 'PM', NULL, NULL), (15, 4, 'PM', '2026-11-02', '2026-11-10')");
        JsonNode d = getAs("/api/v1/dashboard", "demo04").data();
        assertThat(d.get("has_current_pm_assignment").asBoolean()).isTrue();
        assertThat(texts(d.get("upcoming_pm_projects"), "project_name")).containsExactly("D-Day", "D-7 대상");
        assertThat(d.at("/upcoming_pm_projects/0/days_left").asLong()).isZero();
        assertThat(d.at("/upcoming_pm_projects/1/days_left").asLong()).isEqualTo(7);
        assertThat(d.at("/metrics/upcoming_project_count").asLong()).isEqualTo(2);
    }

    @Test
    void 토큰이_없으면_401() {
        assertThat(getAs("/api/v1/dashboard", null).status()).isEqualTo(401);
    }

    @Test
    void 기준일_형식이_틀리면_400() {
        assertThat(getAs("/api/v1/dashboard?as_of=2026-13-01", "demo01").status()).isEqualTo(400);
    }

    @Test
    void 과거_기준일에도_당시_예정일이_지난_진행_프로젝트는_경과로_본다() {
        JsonNode d = getAs("/api/v1/dashboard?as_of=2026-07-01", "demo01").data();
        assertThat(texts(d.get("overdue_projects"), "project_name")).containsExactly("연구 데이터 관리 시스템");
        assertThat(d.at("/ongoing_projects/0/overdue").asBoolean()).isTrue();
        assertThat(getAs("/api/v1/dashboard?as_of=2026-06-30", "demo01").data().get("overdue_projects")).as("예정 종료일 당일은 경과 아님").isEmpty();
    }
}
