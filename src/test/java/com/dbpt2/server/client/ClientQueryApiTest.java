package com.dbpt2.server.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientQueryApiTest extends IntegrationTest {

    private List<Integer> ids(Response r) {
        List<Integer> out = new ArrayList<>();
        r.data().forEach(n -> out.add(n.get("client_id").asInt()));
        return out;
    }

    @Test
    void 경영진은_전체_발주처와_프로젝트_수를_본다() {
        Response r = getAs("/api/v1/clients", "demo01").assertNoSecrets();
        assertThat(ids(r)).containsExactly(1, 2, 3);
        assertThat(r.data().get(0).get("project_count").asLong()).isEqualTo(2);
        assertThat(r.data().get(1).get("contact_phone_number").isNull()).isTrue();
        assertThat(ids(getAs("/api/v1/clients?query=한빛", "demo01"))).containsExactly(1);
        assertThat(ids(getAs("/api/v1/clients?query=이담당", "demo01"))).containsExactly(2);
    }

    @Test
    void 직원은_소속_프로젝트의_발주처만_본다() {
        Response r = getAs("/api/v1/clients", "demo03");
        assertThat(ids(r)).containsExactly(1);
        assertThat(r.data().get(0).get("project_count").asLong()).isEqualTo(2);
        Response demo02 = getAs("/api/v1/clients", "demo02");
        assertThat(ids(demo02)).containsExactly(1, 3);
        assertThat(demo02.data().get(0).get("project_count").asLong()).as("본인 소속 프로젝트만 센다").isEqualTo(1);
    }

    @Test
    void 상세는_경영진_또는_해당_발주처_프로젝트_구성원() {
        assertThat(getAs("/api/v1/clients/2", "demo03").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/clients/1", "demo03").status()).isEqualTo(200);
        assertThat(getAs("/api/v1/clients/3", "demo01").data().get("project_count").asLong()).isEqualTo(1);
        assertThat(getAs("/api/v1/clients/999", "demo01").status()).isEqualTo(404);
        assertThat(getAs("/api/v1/clients/1", null).status()).isEqualTo(401);
    }
}
