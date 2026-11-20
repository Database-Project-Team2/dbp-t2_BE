package com.dbpt2.server.common;

import java.util.List;

/** 오류 응답 `{error: {code, message, field_errors}, meta}`. */
public record ErrorResponse(Body error, ApiResponse.Meta meta) {

    public static ErrorResponse of(ErrorCode code, String message, List<ApiException.FieldError> fieldErrors) {
        return new ErrorResponse(new Body(code.name(), message, fieldErrors), ApiResponse.Meta.current());
    }

    public record Body(String code, String message, List<ApiException.FieldError> fieldErrors) {}
}
