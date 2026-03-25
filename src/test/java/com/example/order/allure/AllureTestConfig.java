package com.example.order.allure;

import com.example.order.allure.assertion.AllureAssertInstrumentation;
import com.example.order.allure.db.AllureRepositoryAspect;
import com.example.order.allure.http.AllureHttpResultHandler;
import com.example.order.allure.http.AllureRestAssuredFilter;
import com.example.order.allure.kafka.AllureKafkaInstrumentation;
import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AllureTestConfig {

    @PostConstruct
    void installInstrumentation() {
        AllureKafkaInstrumentation.install();
        AllureAssertInstrumentation.install();
        RestAssured.filters(new AllureRestAssuredFilter());
    }

    @Bean
    public MockMvcBuilderCustomizer allureMockMvcCustomizer() {
        return builder -> builder.alwaysDo(new AllureHttpResultHandler());
    }

    @Bean
    public AllureRepositoryAspect allureRepositoryAspect() {
        return new AllureRepositoryAspect();
    }
}
