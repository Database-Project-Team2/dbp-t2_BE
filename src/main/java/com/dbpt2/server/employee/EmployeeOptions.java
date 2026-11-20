package com.dbpt2.server.employee;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** 프론트 employeeOptions.ts·registration.ts와 같은 선택지·형식. */
public final class EmployeeOptions {

    public static final List<String> EDUCATIONS = List.of("고졸", "전문대졸(초대졸)", "대졸(학사)", "석사", "박사");
    public static final Pattern LOGIN_ID = Pattern.compile("^[a-zA-Z0-9._-]{3,100}$");
    public static final int PASSWORD_MIN = 8;
    /** BCrypt는 72바이트까지만 반영한다. */
    public static final int PASSWORD_MAX_BYTES = 72;

    private EmployeeOptions() {}

    /** login_id는 대소문자를 구분하지 않는다. 저장·비교 모두 이 값으로 한다. */
    public static String normalizeLoginId(String loginId) {
        return Normalizer.normalize(loginId.trim(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }
}
