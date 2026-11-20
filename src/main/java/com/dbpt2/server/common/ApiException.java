package com.dbpt2.server.common;

import java.util.List;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final List<FieldError> fieldErrors;

    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    public ApiException(ErrorCode code, String message) {
        this(code, message, List.of());
    }

    public ApiException(ErrorCode code, String message, List<FieldError> fieldErrors) {
        super(message);
        this.code = code;
        this.fieldErrors = fieldErrors;
    }

    public static ApiException field(ErrorCode code, String field, String message) {
        return new ApiException(code, message, List.of(new FieldError(field, message)));
    }

    public static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND);
    }

    public static ApiException forbidden() {
        return new ApiException(ErrorCode.FORBIDDEN);
    }

    /** 업무 조건 위반(422). */
    public static ApiException rule(String message) {
        return new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public static ApiException invalid(String field, String message) {
        return field(ErrorCode.INVALID_REQUEST, field, message);
    }

    public ErrorCode code() {
        return code;
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }

    public record FieldError(String field, String message) {}
}
