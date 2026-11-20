package com.dbpt2.server.common;

import org.springframework.http.HttpStatus;

/** 응답 error.code와 HTTP 상태. 메시지는 화면에 그대로 보여 줄 수 있는 문장으로 둔다. */
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값을 확인해 주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "로그인 ID 또는 비밀번호를 확인해 주세요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "요청 권한이 없어요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없어요."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식이에요."),
    LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID예요."),
    MEMBER_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 직원이에요. 동일 프로젝트 재투입은 지원하지 않아요."),
    EVALUATION_DUPLICATED(HttpStatus.CONFLICT, "이미 제출한 평가가 있어요."),
    PROJECT_CLOSED(HttpStatus.CONFLICT, "종료된 프로젝트는 변경할 수 없어요."),
    ASSIGNMENT_ENDED(HttpStatus.CONFLICT, "투입이 종료된 구성원은 기간을 변경할 수 없어요."),
    WORKFLOW_CONFLICT(HttpStatus.CONFLICT, "고객평가 진행 단계와 맞지 않는 요청이에요."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "다른 데이터와 충돌해 저장하지 못했어요."),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "업무 조건을 만족하지 않아요."),
    EMPLOYEE_NO_EXHAUSTED(HttpStatus.UNPROCESSABLE_ENTITY, "해당 연도의 사번 발급 한도를 초과했어요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 요청을 처리하지 못했어요.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
