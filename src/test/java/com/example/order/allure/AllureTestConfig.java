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

import java.util.concurrent.atomic.AtomicBoolean;

@TestConfiguration
public class AllureTestConfig {

    private static final AtomicBoolean INSTRUMENTATION_INSTALLED = new AtomicBoolean(false);

    @PostConstruct
    void installInstrumentation() {
        if (!INSTRUMENTATION_INSTALLED.compareAndSet(false, true)) {
            return;
        }
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
