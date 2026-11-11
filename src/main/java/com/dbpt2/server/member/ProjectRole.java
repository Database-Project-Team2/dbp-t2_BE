package com.dbpt2.server.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectRole {
    PM("PM"),
    PL("PL"),
    ANALYST("분석가"),
    DESIGNER("설계자"),
    PROGRAMMER("프로그래머"),
    TESTER("테스터");

    private final String label;
}
