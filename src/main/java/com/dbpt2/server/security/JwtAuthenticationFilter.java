package com.dbpt2.server.security;

import com.dbpt2.server.employee.EmployeeRepository;
import com.dbpt2.server.employee.EmploymentStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bearer 토큰을 검증하고, 퇴사자가 아니면 현재 권한으로 인증 정보를 만든다. 실패하면 비인증으로 둔다(→ 401). */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.parseEmployeeId(header.substring(7).trim())
                    .flatMap(employeeRepository::findById)
                    .filter(employee -> employee.getEmploymentStatus() != EmploymentStatus.RESIGNED)
                    .ifPresent(employee -> {
                        AuthUser user = new AuthUser(employee.getId(), employee.getSystemRole());
                        var authentication = new UsernamePasswordAuthenticationToken(user, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + employee.getSystemRole().name())));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        chain.doFilter(request, response);
    }
}
