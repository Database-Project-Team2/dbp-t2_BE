package com.dbpt2.server.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientCommandApiTest extends IntegrationTest {

    static Map<String, Object> client(Map<String, Object> overrides) {
        Map<String, Object> body = new HashMap<>(Map.of("client_name", "가상 푸른물산", "contact_name", "최담당",
                "contact_email", "blue@example.com", "contact_phone_number", "02-000-0004"));
        body.putAll(overrides);
        return body;
    }

    @Test
    void 경영진은_발주처를_등록하고_수정한다() {
        Response created = postAs("/api/v1/clients", "demo01", client(Map.of()));
        assertThat(created.status()).isEqualTo(201);
        int id = created.data().get("client_id").asInt();
        assertThat(created.data().get("project_count").asLong()).isZero();
        Response updated = patchAs("/api/v1/clients/" + id, "demo01", Map.of("contact_name", "정담당"));
        assertThat(updated.data().get("contact_name").asText()).isEqualTo("정담당");
        assertThat(updated.data().get("contact_email").asText()).isEqualTo("blue@example.com");
        Map<String, Object> clearPhone = new HashMap<>();
        clearPhone.put("contact_phone_number", null);
        assertThat(patchAs("/api/v1/clients/" + id, "demo01", clearPhone).data().get("contact_phone_number").isNull()).isTrue();
    }

    @Test
    void 담당자를_바꾸면_과거_프로젝트_상세에도_현재_담당자가_보인다() {
        patchAs("/api/v1/clients/1", "demo01", Map.of("contact_name", "한담당", "contact_email", "new@example.com"));
        assertThat(getAs("/api/v1/projects/1", "demo03").data().at("/client/contact_name").asText()).isEqualTo("한담당");
    }

    @Test
    void 일반_직원과_PM은_발주처를_바꿀_수_없다() {
        assertThat(postAs("/api/v1/clients", "demo03", client(Map.of())).status()).isEqualTo(403);
        assertThat(patchAs("/api/v1/clients/1", "demo05", Map.of("contact_name", "x")).status()).isEqualTo(403);
        assertThat(postAs("/api/v1/clients", null, client(Map.of())).status()).isEqualTo(401);
    }

    @Test
    void 입력_위반은_400_없는_발주처는_404() {
        assertThat(postAs("/api/v1/clients", "demo01", client(Map.of("client_name", " "))).status()).isEqualTo(400);
        assertThat(postAs("/api/v1/clients", "demo01", client(Map.of("contact_email", "not-an-email"))).status()).isEqualTo(400);
        assertThat(postAs("/api/v1/clients", "demo01", client(Map.of("contact_phone_number", "전화"))).status()).isEqualTo(400);
        assertThat(postAs("/api/v1/clients", "demo01", client(Map.of("client_id", 9))).status()).isEqualTo(400);
        Map<String, Object> nullName = new HashMap<>();
        nullName.put("client_name", null);
        assertThat(patchAs("/api/v1/clients/1", "demo01", nullName).status()).isEqualTo(400);
        assertThat(patchAs("/api/v1/clients/999", "demo01", Map.of("contact_name", "x")).status()).isEqualTo(404);
    }
}
