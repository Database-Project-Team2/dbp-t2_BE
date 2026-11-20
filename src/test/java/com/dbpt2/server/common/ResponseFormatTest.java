package com.dbpt2.server.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class ResponseFormatTest extends IntegrationTest {

    @Test
    void 없는_경로는_오류_형식의_404() {
        Response response = getAs("/api/v1/nothing-here", "demo01");
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.errorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.json().at("/error/field_errors").isArray()).isTrue();
        assertThat(response.json().at("/meta/request_id").asText()).isNotBlank();
    }

    @Test
    void 인증이_필요한_경로를_토큰_없이_부르면_오류_형식의_401() {
        Response response = getAs("/api/v1/nothing-here", null);
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.errorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void 깨진_JSON은_400() {
        Response response = call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                .contentType("application/json").content("{\"login_id\":"), null, null);
        assertThat(response.status()).isEqualTo(400);
    }

    @Test
    void Swagger_문서에_Bearer_인증이_있다() {
        Response docs = getAs("/v3/api-docs", null);
        assertThat(docs.status()).isEqualTo(200);
        assertThat(docs.json().at("/components/securitySchemes/bearer/scheme").asText()).isEqualTo("bearer");
        assertThat(docs.body()).contains("/api/v1/auth/login");
        assertThat(getAs("/swagger-ui/index.html", null).status()).isEqualTo(200);
    }
}
