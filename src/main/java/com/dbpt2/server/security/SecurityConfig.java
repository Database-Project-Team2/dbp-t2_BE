package com.dbpt2.server.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** 공개: 로그인·ID 확인·회원가입·헬스체크·Swagger. 나머지는 모두 토큰 필요. 세부 권한은 AccessPolicy에서 검사. */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                            SecurityErrorWriter errors) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errors.entryPoint())
                        .accessDeniedHandler(errors.accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/employees").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/check-login-id").permitAll()
                        .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 경영진 전용 API는 본문 검사보다 먼저 막는다(권한 없는 사용자에게 400 대신 403).
                        .requestMatchers(HttpMethod.GET, "/api/v1/employees", "/api/v1/assignments", "/api/v1/assignments/export.csv",
                                "/api/v1/projects/*/client-options", "/api/v1/projects/*/member-candidates").hasRole("MANAGEMENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects", "/api/v1/projects/*/close", "/api/v1/projects/*/members",
                                "/api/v1/clients").hasRole("MANAGEMENT")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/projects/*", "/api/v1/members/*/assignment",
                                "/api/v1/clients/*").hasRole("MANAGEMENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/members/*").hasRole("MANAGEMENT")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 배포 프론트와 로컬 Vite만 허용한다. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition", "X-Request-Id"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
