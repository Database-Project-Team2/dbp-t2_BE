package com.dbpt2.server.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Set;

/**
 * PATCH 본문: 보낸 필드만 수정한다. 필드가 없으면 그대로, null이면 비우기(nullable 필드만 허용).
 * 레코드로는 "없음"과 "null"을 구분할 수 없어 JSON 트리를 직접 읽는다.
 */
public final class PatchBody {

    private final JsonNode node;

    public PatchBody(JsonNode node, Set<String> allowed) {
        if (node == null || !node.isObject()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "요청 본문은 JSON 객체여야 해요.");
        }
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            String field = it.next();
            if (!allowed.contains(field)) {
                throw ApiException.invalid(field, field + "은(는) 수정할 수 없는 필드예요.");
            }
        }
        this.node = node;
    }

    public boolean has(String field) {
        return node.has(field);
    }

    public boolean isEmpty() {
        return node.isEmpty();
    }

    /** 문자열. nullable이 아니면 null을 400으로 거부한다. 앞뒤 공백은 잘라 돌려준다. */
    public String text(String field, boolean nullable) {
        JsonNode value = node.get(field);
        if (value.isNull()) {
            if (!nullable) {
                throw ApiException.invalid(field, field + "은(는) 비울 수 없어요.");
            }
            return null;
        }
        if (!value.isTextual()) {
            throw ApiException.invalid(field, field + " 값의 형식이 올바르지 않아요.");
        }
        return value.asText().trim();
    }

    public LocalDate date(String field, boolean nullable) {
        String text = text(field, nullable);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field, field + "은(는) YYYY-MM-DD 형식이어야 해요.");
        }
    }

    public int integer(String field) {
        JsonNode value = node.get(field);
        if (value.isNull() || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw ApiException.invalid(field, field + " 값의 형식이 올바르지 않아요.");
        }
        return value.asInt();
    }
}
