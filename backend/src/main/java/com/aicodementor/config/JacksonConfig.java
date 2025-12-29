package com.aicodementor.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    
    /**
     * Customize Jackson ObjectMapper to properly serialize LocalDateTime
     * This approach works with Spring Boot's auto-configuration instead of overriding it
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            // Disable writing dates as timestamps (use ISO-8601 format instead)
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // JavaTimeModule is automatically registered by Spring Boot
            // LocalDateTime will be serialized in ISO-8601 format (e.g., "2025-12-29T18:01:50")
        };
    }
}

