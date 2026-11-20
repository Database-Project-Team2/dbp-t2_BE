package com.dbpt2.server.common;

public final class JsonNames {

    private JsonNames() {}

    /** camelCase 필드명을 응답 형식(snake_case)으로 바꾼다. */
    public static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
