package com.dbpt2.server.common;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException e) {
        return respond(e.code(), e.getMessage(), e.fieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException e) {
        List<ApiException.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiException.FieldError(JsonNames.snake(f.getField()), f.getDefaultMessage()))
                .toList();
        String message = fields.isEmpty() ? ErrorCode.INVALID_REQUEST.defaultMessage() : fields.get(0).message();
        return respond(ErrorCode.INVALID_REQUEST, message, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> invalidParameter(HandlerMethodValidationException e) {
        return respond(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.defaultMessage(), List.of());
    }

    /** 허용하지 않은 필드(system_role, employee_no 등)·자료형 오류·깨진 JSON. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        if (cause instanceof UnrecognizedPropertyException unknown) {
            String field = unknown.getPropertyName();
            return respond(ErrorCode.INVALID_REQUEST, field + "은(는) 보낼 수 없는 필드예요.",
                    List.of(new ApiException.FieldError(field, "허용되지 않는 필드예요.")));
        }
        if (cause instanceof InvalidFormatException || cause instanceof MismatchedInputException) {
            MismatchedInputException mismatch = (MismatchedInputException) cause;
            String field = mismatch.getPath().isEmpty() ? "body" : mismatch.getPath().get(mismatch.getPath().size() - 1).getFieldName();
            return respond(ErrorCode.INVALID_REQUEST, field + " 값의 형식이 올바르지 않아요.",
                    List.of(new ApiException.FieldError(field, "형식이 올바르지 않아요.")));
        }
        return respond(ErrorCode.INVALID_REQUEST, "요청 본문을 읽을 수 없어요.", List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParameter(MissingServletRequestParameterException e) {
        return respond(ErrorCode.INVALID_REQUEST, e.getParameterName() + " 값을 입력해 주세요.",
                List.of(new ApiException.FieldError(e.getParameterName(), "필수 값이에요.")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(ErrorCode.INVALID_REQUEST, e.getName() + " 값의 형식이 올바르지 않아요.",
                List.of(new ApiException.FieldError(e.getName(), "형식이 올바르지 않아요.")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> noResource(NoResourceFoundException e) {
        return respond(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.defaultMessage(), List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> method(HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.defaultMessage(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> denied(AccessDeniedException e) {
        return respond(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage(), List.of());
    }

    /** 서버 검사를 통과했지만 DB 제약에 걸린 경우(동시 요청 등). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e) {
        log.warn("DB 제약 위반: {}", e.getMostSpecificCause().getMessage());
        return respond(ErrorCode.DATA_CONFLICT, ErrorCode.DATA_CONFLICT.defaultMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unknown(Exception e) {
        log.error("처리하지 못한 오류", e);
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), List.of());
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, String message, List<ApiException.FieldError> fields) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, message, fields));
    }
}
