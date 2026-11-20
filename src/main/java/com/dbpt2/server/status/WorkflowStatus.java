package com.dbpt2.server.status;

/** 고객평가 진행: 의뢰 전 → 회수 중(의뢰) → 보고 대기(회수) → 보고 완료. */
public enum WorkflowStatus { NOT_REQUESTED, COLLECTING, AWAITING_REPORT, REPORTED }
