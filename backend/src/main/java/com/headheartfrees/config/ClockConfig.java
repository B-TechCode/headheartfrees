package com.headheartfrees.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A {@link Clock} bean so services take time as a dependency rather than
 * calling {@code Instant.now()} directly. Makes anything time-dependent
 * testable without sleeping.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
