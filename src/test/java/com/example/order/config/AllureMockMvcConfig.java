package com.example.order.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AllureMockMvcConfig {

    @PostConstruct
    void instrumentKafka() {
        AllureKafkaInstrumentation.install();
    }

    @Bean
    public MockMvcBuilderCustomizer allureMockMvcCustomizer() {
        return builder -> builder.alwaysDo(new AllureMockMvcResultHandler());
    }

    @Bean
    public AllureRepositoryAspect allureRepositoryAspect() {
        return new AllureRepositoryAspect();
    }
}
