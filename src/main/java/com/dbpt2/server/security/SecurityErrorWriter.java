package com.dbpt2.server.security;

import com.dbpt2.server.common.ErrorCode;
import com.dbpt2.server.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 필터 단계의 401/403도 컨트롤러와 같은 오류 형식으로 쓴다. */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    AuthenticationEntryPoint entryPoint() {
        return (request, response, e) -> write(response, ErrorCode.UNAUTHORIZED);
    }

    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, e) -> write(response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(code, code.defaultMessage(), List.of()));
    }
}
