package com.example.order.config;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AllureMockMvcConfig {

    @Bean
    public MockMvcBuilderCustomizer allureMockMvcCustomizer() {
        return builder -> builder.alwaysDo(new AllureMockMvcResultHandler());
    }
}
