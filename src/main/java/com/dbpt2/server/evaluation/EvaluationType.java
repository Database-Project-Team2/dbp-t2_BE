package com.dbpt2.server.evaluation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EvaluationType {
    CLIENT("고객평가"),
    PM("PM 평가"),
    PEER("동료평가");

    private final String label;
}
