package com.dbpt2.server.dashboard;

import com.dbpt2.server.common.ApiResponse;
import com.dbpt2.server.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "업무 현황")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "업무 현황", description = "view=MANAGEMENT(경영진) 또는 EMPLOYEE(직원) 형태로 응답")
    @GetMapping
    public ApiResponse<Object> dashboard(@AuthenticationPrincipal AuthUser user,
                                         @RequestParam(name = "as_of", required = false) LocalDate asOf) {
        return ApiResponse.of(dashboardService.dashboard(user, asOf));
    }
}
