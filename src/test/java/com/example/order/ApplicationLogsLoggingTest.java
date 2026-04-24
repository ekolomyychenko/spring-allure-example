package com.example.order;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@Epic("Allure instrumentation")
@Feature("Application logs capture (Logback)")
class ApplicationLogsLoggingTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ApplicationLogsLoggingTest.class);

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Logback appender captures SLF4J logs emitted during the test")
    void shouldAttachApplicationLogs() {
        log.info("Test log line at INFO level");
        log.warn("Test log line at WARN level with param={}", 42);

        Order order = new Order();
        order.setProductName("laptop");
        order.setQuantity(1);
        order.setPrice(new BigDecimal("999.99"));
        order.setStatus(OrderStatus.PRICED);
        orderRepository.save(order);

        log.info("Done saving order");
    }
}
