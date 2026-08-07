package com.qiyam.shared.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class RestConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    /** Injectable system clock — lets time-dependent services (e.g. next-prayer calculation) be unit tested with a fixed instant. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
