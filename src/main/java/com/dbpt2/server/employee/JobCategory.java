package com.dbpt2.server.employee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobCategory {
    DEVELOPER("개발"),
    MARKETING("마케팅"),
    BUSINESS_ADMIN("경영관리"),
    RESEARCH("연구개발");

    private final String label;
}
