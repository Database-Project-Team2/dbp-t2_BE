package com.dbpt2.server.employee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmploymentStatus {
    ACTIVE("재직"),
    ON_LEAVE("휴직"),
    RESIGNED("퇴사");

    private final String label;
}
