package com.dbpt2.server.security;

import com.dbpt2.server.employee.SystemRole;

/** 인증된 요청의 사용자. 토큰을 받을 때마다 DB에서 현재 권한·재직 상태를 다시 읽어 만든다. */
public record AuthUser(int employeeId, SystemRole systemRole) {

    public boolean isManagement() {
        return systemRole == SystemRole.MANAGEMENT;
    }
}
