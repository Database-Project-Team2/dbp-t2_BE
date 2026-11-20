package com.dbpt2.server.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 업무 기준일은 Asia/Seoul의 오늘. 테스트는 Clock 빈을 바꿔 날짜를 고정한다. */
@Configuration
public class TimeConfig {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    Clock clock() {
        return Clock.system(ZONE);
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }
}
