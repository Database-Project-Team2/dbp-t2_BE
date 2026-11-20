package com.dbpt2.server.crypto;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;

/** 프론트 shared/domain/residentNumber.ts와 같은 형식 검사. 체크섬·본인인증은 하지 않는다. */
public final class ResidentNumberRule {

    private static final Pattern FORMAT = Pattern.compile("^\\d{6}-?\\d{7}$");
    private static final Map<Character, Integer> CENTURIES =
            Map.of('9', 1800, '0', 1800, '1', 1900, '2', 1900, '3', 2000, '4', 2000);

    private ResidentNumberRule() {}

    public static boolean isValid(String value, LocalDate today) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            return false;
        }
        String digits = value.replace("-", "");
        Integer century = CENTURIES.get(digits.charAt(6));
        if (century == null) {
            return false;
        }
        try {
            LocalDate birth = LocalDate.of(
                    century + Integer.parseInt(digits.substring(0, 2)),
                    Integer.parseInt(digits.substring(2, 4)),
                    Integer.parseInt(digits.substring(4, 6)));
            return !birth.isAfter(today);
        } catch (DateTimeException e) {
            return false;
        }
    }

    /** 저장 전 표준형 YYMMDD-NNNNNNN. */
    public static String normalize(String value) {
        String digits = value.replace("-", "");
        return digits.substring(0, 6) + "-" + digits.substring(6);
    }
}
