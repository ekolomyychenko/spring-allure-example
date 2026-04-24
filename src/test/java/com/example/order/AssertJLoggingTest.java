package com.example.order;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Allure instrumentation")
@Feature("AssertJ — fluent assertions")
class AssertJLoggingTest extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("AssertJ logs a simple isEqualTo assertion")
    void shouldLogAssertJIsEqualTo() {
        Order saved = orderRepository.save(order("laptop", new BigDecimal("999.99")));

        assertThat(saved.getProductName()).isEqualTo("laptop");
    }

    @Test
    @DisplayName("AssertJ logs chained string assertions")
    void shouldLogAssertJChainedStringAssertions() {
        Order saved = orderRepository.save(order("laptop", new BigDecimal("999.99")));

        assertThat(saved.getProductName())
                .startsWith("lap")
                .endsWith("top")
                .containsIgnoringCase("LAPT");
    }

    @Test
    @DisplayName("AssertJ logs numeric range and offset assertions")
    void shouldLogAssertJNumericAssertions() {
        Order saved = orderRepository.save(order("phone", new BigDecimal("599.00")));

        assertThat(saved.getQuantity()).isPositive().isBetween(1, 10);
        assertThat(saved.getPrice())
                .isGreaterThan(BigDecimal.ZERO)
                .isCloseTo(new BigDecimal("599.00"), Offset.offset(new BigDecimal("0.01")));
    }

    @Test
    @DisplayName("AssertJ logs collection assertions — hasSize / allMatch / anyMatch / noneMatch")
    void shouldLogAssertJCollectionAssertions() {
        orderRepository.save(order("laptop", new BigDecimal("999.99")));
        orderRepository.save(order("phone", new BigDecimal("599.00")));

        List<Order> orders = orderRepository.findAll();

        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getProductName() != null);
        assertThat(orders).anyMatch(o -> o.getProductName().equals("laptop"));
        assertThat(orders).noneMatch(o -> o.getProductName().equals("nonexistent"));
    }

    @Test
    @DisplayName("AssertJ logs enum assertions — isEqualTo / isInstanceOf / isIn")
    void shouldLogAssertJEnumAssertions() {
        Order saved = orderRepository.save(order("mouse", new BigDecimal("25.00")));

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PRICED);
        assertThat(saved.getStatus()).isInstanceOf(OrderStatus.class);
        assertThat(saved.getStatus()).isIn(OrderStatus.PRICED, OrderStatus.NEW);
    }

    private Order order(String productName, BigDecimal price) {
        Order order = new Order();
        order.setProductName(productName);
        order.setQuantity(1);
        order.setPrice(price);
        order.setStatus(OrderStatus.PRICED);
        return order;
    }
}
