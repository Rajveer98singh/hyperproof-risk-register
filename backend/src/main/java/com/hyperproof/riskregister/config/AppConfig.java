package com.hyperproof.riskregister.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** System UTC clock; injected where "today" is needed so tests can supply a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
