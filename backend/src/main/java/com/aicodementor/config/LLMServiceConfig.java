package com.aicodementor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LLMServiceConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // ObjectMapper is now provided by JacksonConfig with proper LocalDateTime serialization
    // No need to create a separate bean here

    @Bean
    public String llamacppBaseUrl(@Value("${llm.llamacpp.base-url:http://localhost:11435}") String baseUrl) {
        return baseUrl != null ? baseUrl : "http://localhost:11435";
    }
}

