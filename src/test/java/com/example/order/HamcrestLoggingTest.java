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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Epic("Allure instrumentation")
@Feature("Hamcrest — assertThat")
class HamcrestLoggingTest extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Hamcrest logs 2-arg assertThat(actual, matcher)")
    void shouldLogHamcrest2Arg() {
        Order saved = orderRepository.save(order("mouse", new BigDecimal("25.00")));

        assertThat(saved.getProductName(), is("mouse"));
        assertThat(saved.getQuantity(), equalTo(4));
        assertThat(saved.getPrice(), notNullValue());
        assertThat(saved.getPrice().intValue(), greaterThan(0));
    }

    @Test
    @DisplayName("Hamcrest logs 3-arg assertThat(reason, actual, matcher)")
    void shouldLogHamcrest3Arg() {
        Order saved = orderRepository.save(order("mouse", new BigDecimal("25.00")));

        assertThat("Product name should match", saved.getProductName(), is("mouse"));
        assertThat("Status should be PRICED", saved.getStatus().name(), equalTo("PRICED"));
    }

    private Order order(String productName, BigDecimal price) {
        Order order = new Order();
        order.setProductName(productName);
        order.setQuantity(4);
        order.setPrice(price);
        order.setStatus(OrderStatus.PRICED);
        return order;
    }
}
