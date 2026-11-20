package com.dbpt2.server.common;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** CSV 다운로드: UTF-8 BOM(엑셀 한글), CRLF, 모든 칸 따옴표, 수식 주입 방지(=,+,-,@로 시작하면 ' 붙임). */
public final class Csv {

    private static final Pattern FORMULA = Pattern.compile("^[=+@\\-\\t\\r].*", Pattern.DOTALL);

    private Csv() {}

    public static String content(List<String> headers, List<List<Object>> rows) {
        StringBuilder out = new StringBuilder("﻿");
        out.append(line(headers.stream().map(h -> (Object) h).toList()));
        for (List<Object> row : rows) {
            out.append("\r\n").append(line(row));
        }
        return out.toString();
    }

    private static String line(List<Object> cells) {
        return cells.stream().map(Csv::cell).collect(Collectors.joining(","));
    }

    private static String cell(Object value) {
        String s = value == null ? "" : value.toString();
        if (FORMULA.matcher(s).matches()) {
            s = "'" + s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    public static ResponseEntity<byte[]> download(String filename, String content) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"download.csv\"; filename*=UTF-8''" + encoded)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(content.getBytes(StandardCharsets.UTF_8));
    }
}
