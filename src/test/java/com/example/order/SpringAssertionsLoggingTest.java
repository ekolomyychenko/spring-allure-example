package com.example.order;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertNotEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@Epic("Allure instrumentation")
@Feature("Spring AssertionErrors")
class SpringAssertionsLoggingTest extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Spring logs assertEquals / assertNotEquals")
    void shouldLogAssertEqualsAndNotEquals() {
        Order saved = orderRepository.save(order("headphones", new BigDecimal("79.99")));

        assertEquals("Product name", "headphones", saved.getProductName());
        assertNotEquals("Price should not be zero", BigDecimal.ZERO, saved.getPrice());
    }

    @Test
    @DisplayName("Spring logs assertTrue / assertFalse")
    void shouldLogAssertTrueAndFalse() {
        Order saved = orderRepository.save(order("headphones", new BigDecimal("79.99")));

        assertTrue("Quantity should be positive", saved.getQuantity() > 0);
        assertFalse("Price should not be zero", saved.getPrice().signum() == 0);
    }

    @Test
    @DisplayName("Spring logs assertNull / assertNotNull")
    void shouldLogAssertNullAndNotNull() {
        Order saved = orderRepository.save(order("headphones", new BigDecimal("79.99")));

        assertNotNull("Order should have an id", saved.getId());
        assertNull("Unrelated reference should be null", null);
    }

    private Order order(String productName, BigDecimal price) {
        Order order = new Order();
        order.setProductName(productName);
        order.setQuantity(2);
        order.setPrice(price);
        order.setStatus(OrderStatus.PRICED);
        return order;
    }
}
