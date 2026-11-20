package com.dbpt2.server.support;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 테스트 기준일을 2026-11-20(Asia/Seoul)로 고정한다. 시드 날짜가 이 날짜를 기준으로 짜여 있다. */
@TestConfiguration
public class FixedClockConfig {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final ZonedDateTime NOW = ZonedDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZONE);

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW.toInstant(), ZONE);
    }
}
