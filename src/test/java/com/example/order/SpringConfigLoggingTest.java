package com.example.order;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("Allure instrumentation")
@Feature("Spring configuration snapshot")
class SpringConfigLoggingTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Spring environment properties are attached to the report before the test runs")
    void shouldAttachSpringConfig() {
    }
}
