package com.dbpt2.server.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CareerApiTest extends IntegrationTest {

    private List<Integer> ids(Response r) {
        List<Integer> out = new ArrayList<>();
        r.data().forEach(n -> out.add(n.get("member_id").asInt()));
        return out;
    }

    @Test
    void 본인은_실제_투입_이력만_경영진은_투입_전까지_본다() {
        jdbc.update("INSERT INTO PROJECT_MEMBER (member_id, project_id, employee_id, project_role) VALUES (60, 3, 4, 'TESTER')");
        Response self = getAs("/api/v1/employees/4/career", "demo04").assertNoSecrets();
        assertThat(ids(self)).as("최수빈 본인: P3 투입 전 배정 제외").containsExactlyInAnyOrder(4, 18);
        assertThat(ids(getAs("/api/v1/employees/4/career", "demo01"))).containsExactlyInAnyOrder(4, 18, 60);
        Response mine = getAs("/api/v1/employees/3/career", "demo03");
        assertThat(ids(mine)).containsExactlyInAnyOrder(3, 17);
        assertThat(mine.data().get(0).at("/project/project_name").asText()).isNotBlank();
    }

    @Test
    void 다른_직원_경력은_404() {
        assertThat(getAs("/api/v1/employees/2/career", "demo03").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/employees/2/career.csv", "demo03").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/employees/999/career", "demo01").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/employees/3/career", null).status()).isEqualTo(401);
    }

    @Test
    void 경력_필터() {
        assertThat(ids(getAs("/api/v1/employees/8/career?as_of=2026-05-01", "demo01"))).containsExactly(9);
        assertThat(ids(getAs("/api/v1/employees/8/career?project_role=ANALYST", "demo01"))).hasSize(3);
        assertThat(ids(getAs("/api/v1/employees/8/career?ongoing_projects_only=true", "demo01"))).containsExactly(14);
        assertThat(ids(getAs("/api/v1/employees/8/career?from=2026-01-01&to=2026-02-01", "demo01"))).containsExactly(5);
    }

    @Test
    void 경력_CSV() {
        Response csv = getAs("/api/v1/employees/3/career.csv", "demo03");
        assertThat(csv.status()).isEqualTo(200);
        assertThat(csv.contentType()).startsWith("text/csv");
        assertThat(csv.body()).startsWith("﻿\"사번\"").contains("유통 분석 대시보드").contains("20240003");
        assertThat(csv.body().split("\r\n")).hasSize(3);
        assertThat(csv.contentDisposition()).contains("filename*=UTF-8''");
    }

    @Test
    void 투입_이력_CSV는_경영진_전용이고_같은_조건_전체() {
        assertThat(getAs("/api/v1/assignments/export.csv", "demo03").status()).isEqualTo(403);
        assertThat(getAs("/api/v1/assignments/export.csv", null).status()).isEqualTo(401);
        Response all = getAs("/api/v1/assignments/export.csv", "demo01");
        assertThat(all.body().split("\r\n")).as("헤더 + 20건, page 없음").hasSize(21);
        Response asOf = getAs("/api/v1/assignments/export.csv?as_of=2026-05-01", "demo01");
        assertThat(asOf.body().split("\r\n")).hasSize(6);
        assertThat(asOf.body()).contains("연구 데이터 관리 시스템").doesNotContain("유통 재고관리 시스템");
    }
}
