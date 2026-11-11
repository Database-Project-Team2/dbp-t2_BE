package com.dbpt2.server.employee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemRole {
    EMPLOYEE("일반 직원"),
    MANAGEMENT("경영진");

    private final String label;
}
