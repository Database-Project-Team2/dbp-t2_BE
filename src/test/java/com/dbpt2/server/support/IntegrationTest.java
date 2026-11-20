package com.dbpt2.server.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * API 통합 테스트 기반. 테스트마다 DB를 시드 상태로 되돌리고(docker compose MySQL), 기준일은 2026-11-20으로 고정한다.
 * 시드 계정: demo01 김민준(경영진, P1·P3 PM) / demo03 박지훈(일반, P1 프로그래머·P4 설계자) /
 * demo05 정도윤(P2·P4 PM) / demo09 임현우(휴직, 경영진) / demo10 한지민(퇴사).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
public abstract class IntegrationTest {

    protected static final String TODAY = "2026-11-20";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected DataSource dataSource;
    @Autowired protected JdbcTemplate jdbc;

    private final Map<String, String> tokens = new HashMap<>();

    @BeforeEach
    void resetDatabase() {
        DatabaseReset.reset(dataSource);
        tokens.clear();
    }

    protected String token(String loginId) {
        return tokens.computeIfAbsent(loginId, id -> {
            Response response = call(post("/api/v1/auth/login"), null, Map.of("login_id", id, "password", "demo1234"));
            assertThat(response.status()).as("로그인 " + id).isEqualTo(200);
            return response.json().at("/data/access_token").asText();
        });
    }

    protected Response getAs(String url, String loginId) {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), loginId, null);
    }

    protected Response postAs(String url, String loginId, Object body) {
        return call(post(url), loginId, body);
    }

    protected Response patchAs(String url, String loginId, Object body) {
        return call(patch(url), loginId, body);
    }

    protected Response deleteAs(String url, String loginId) {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url), loginId, null);
    }

    protected Response call(MockHttpServletRequestBuilder builder, String loginId, Object body) {
        try {
            if (loginId != null) {
                builder.header("Authorization", "Bearer " + token(loginId));
            }
            if (body != null) {
                builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(body));
            }
            MvcResult result = mockMvc.perform(builder).andReturn();
            return new Response(result.getResponse().getStatus(), result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                    result.getResponse().getHeader("Content-Disposition"), result.getResponse().getContentType(), objectMapper);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected Response callWithToken(MockHttpServletRequestBuilder builder, String rawToken) {
        if (rawToken != null) {
            builder.header("Authorization", "Bearer " + rawToken);
        }
        return call(builder, null, null);
    }

    public record Response(int status, String body, String contentDisposition, String contentType, ObjectMapper mapper) {

        public JsonNode json() {
            try {
                return mapper.readTree(body);
            } catch (Exception e) {
                throw new IllegalStateException("JSON 아님: " + body, e);
            }
        }

        public JsonNode data() {
            return json().get("data");
        }

        public String errorCode() {
            return json().at("/error/code").asText();
        }

        /** 권한 누수 검사: 비밀번호 해시·주민번호 원문/암호문이 응답 어디에도 없어야 한다. */
        public Response assertNoSecrets() {
            assertThat(body).doesNotContain("password_hash", "passwordHash", "resident_no_encrypted", "aesgcm:", "$2a$", "$2b$");
            assertThat(body).doesNotContainPattern("\\d{6}-[1-4]\\d{6}");
            return this;
        }
    }
}
