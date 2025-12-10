package com.aicodementor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aicodementor")
@EnableJpaRepositories
public class AiCodeMentorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiCodeMentorApplication.class, args);
    }
}

