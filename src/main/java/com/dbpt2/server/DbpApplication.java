package com.dbpt2.server;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DbpApplication {

    public static void main(String[] args) {
        // "오늘" 기준 집계(현재 수행 중 프로젝트, 현재 투입 인원)가 서버 위치와 무관하게 한국 날짜로 계산되도록 고정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(DbpApplication.class, args);
    }
}
