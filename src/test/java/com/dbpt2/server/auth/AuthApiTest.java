package com.dbpt2.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.dbpt2.server.security.JwtService;
import com.dbpt2.server.support.IntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

class AuthApiTest extends IntegrationTest {

    @Autowired JwtService jwtService;
    @Autowired Clock clock;
    @Value("${app.jwt.secret}") String secret;

    private Response login(String id, String password) {
        return postAs("/api/v1/auth/login", null, Map.of("login_id", id, "password", password));
    }

    @Test
    void 로그인하면_토큰_직원_PM여부_서버기준일을_준다() {
        Response response = login("demo05", "demo1234").assertNoSecrets();
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data().get("access_token").asText()).isNotBlank();
        assertThat(response.data().at("/employee/employee_no").asText()).isEqualTo("20240005");
        assertThat(response.data().at("/employee/resident_no_masked").asText()).isEqualTo("880530-*******");
        assertThat(response.data().get("is_pm").asBoolean()).isTrue();
        assertThat(response.data().get("server_today").asText()).isEqualTo(TODAY);
        assertThat(response.json().at("/meta/request_id").asText()).isNotBlank();
        assertThat(java.time.OffsetDateTime.parse(response.json().at("/meta/timestamp").asText()).getOffset().getId()).isEqualTo("+09:00");
    }

    @Test
    void 로그인_ID는_대소문자를_구분하지_않는다() {
        assertThat(login("DEMO01", "demo1234").status()).isEqualTo(200);
    }

    @Test
    void PM_배정이_없으면_is_pm은_false() {
        assertThat(login("demo02", "demo1234").data().get("is_pm").asBoolean()).isFalse();
    }

    @Test
    void 틀린_비밀번호와_없는_ID는_같은_401() {
        Response wrong = login("demo01", "wrong-password");
        Response missing = login("nobody", "demo1234");
        assertThat(wrong.status()).isEqualTo(401);
        assertThat(missing.status()).isEqualTo(401);
        assertThat(wrong.errorCode()).isEqualTo("LOGIN_FAILED").isEqualTo(missing.errorCode());
        assertThat(wrong.json().at("/error/message").asText()).isEqualTo(missing.json().at("/error/message").asText());
    }

    @Test
    void 퇴사자는_로그인할_수_없다() {
        Response response = login("demo10", "demo1234");
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.errorCode()).isEqualTo("LOGIN_FAILED");
    }

    @Test
    void 휴직자는_로그인할_수_있다() {
        assertThat(login("demo09", "demo1234").status()).isEqualTo(200);
    }

    @Test
    void 로그인_필수값이_없으면_400() {
        Response response = postAs("/api/v1/auth/login", null, Map.of("login_id", "demo01"));
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo("INVALID_REQUEST");
        assertThat(response.json().at("/error/field_errors/0/field").asText()).isEqualTo("password");
    }

    @Test
    void 내_정보는_토큰으로_조회한다() {
        Response response = getAs("/api/v1/auth/me", "demo03").assertNoSecrets();
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data().at("/employee/login_id").asText()).isEqualTo("demo03");
        assertThat(response.data().at("/employee/system_role").asText()).isEqualTo("EMPLOYEE");
        assertThat(response.data().get("is_pm").asBoolean()).isFalse();
        assertThat(response.data().get("server_today").asText()).isEqualTo(TODAY);
    }

    @Test
    void 토큰이_없거나_위조되거나_만료되면_401() {
        assertThat(callWithToken(get("/api/v1/auth/me"), null).status()).isEqualTo(401);
        assertThat(callWithToken(get("/api/v1/auth/me"), null).errorCode()).isEqualTo("UNAUTHORIZED");
        assertThat(callWithToken(get("/api/v1/auth/me"), "not-a-jwt").status()).isEqualTo(401);
        String forged = Jwts.builder().subject("1").expiration(Date.from(clock.instant().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor("x".repeat(48).getBytes(StandardCharsets.UTF_8))).compact();
        assertThat(callWithToken(get("/api/v1/auth/me"), forged).status()).isEqualTo(401);
        String expired = Jwts.builder().subject("1")
                .issuedAt(Date.from(clock.instant().minus(Duration.ofHours(13))))
                .expiration(Date.from(clock.instant().minus(Duration.ofHours(1))))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        assertThat(callWithToken(get("/api/v1/auth/me"), expired).status()).isEqualTo(401);
    }

    @Test
    void 토큰_유효기간은_12시간() {
        String token = jwtService.issue(1);
        var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .clock(() -> Date.from(clock.instant())).build().parseSignedClaims(token).getPayload();
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant())).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void 퇴사_처리된_직원의_기존_토큰은_401() {
        String token = jwtService.issue(10);
        assertThat(callWithToken(get("/api/v1/auth/me"), token).status()).isEqualTo(401);
    }

    @Test
    void 로그아웃은_204이고_토큰이_필요하다() {
        assertThat(postAs("/api/v1/auth/logout", "demo01", null).status()).isEqualTo(204);
        assertThat(callWithToken(post("/api/v1/auth/logout"), null).status()).isEqualTo(401);
    }

    @Test
    void ID_중복_확인은_대소문자를_구분하지_않는다() {
        Response taken = getAs("/api/v1/auth/check-login-id?login_id=Demo01", null);
        assertThat(taken.status()).isEqualTo(200);
        assertThat(taken.data().get("available").asBoolean()).isFalse();
        assertThat(taken.data().get("login_id").asText()).isEqualTo("Demo01");
        assertThat(getAs("/api/v1/auth/check-login-id?login_id=new.person", null).data().get("available").asBoolean()).isTrue();
    }

    @Test
    void ID_형식이_틀리거나_없으면_400() {
        assertThat(getAs("/api/v1/auth/check-login-id?login_id=ab", null).status()).isEqualTo(400);
        assertThat(getAs("/api/v1/auth/check-login-id?login_id=한글아이디", null).status()).isEqualTo(400);
        Response missing = getAs("/api/v1/auth/check-login-id", null);
        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.json().at("/error/field_errors/0/field").asText()).isEqualTo("login_id");
    }
}
