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

@Epic("Allure instrumentation")
@Feature("Spring Data JPA — repository calls via AOP")
class JpaRepositoryLoggingTest extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("AOP logs OrderRepository.save(...) call")
    void shouldLogRepositorySave() {
        orderRepository.save(newOrder("laptop"));
    }

    @Test
    @DisplayName("AOP logs OrderRepository.findById(...) call")
    void shouldLogRepositoryFindById() {
        Order saved = orderRepository.save(newOrder("phone"));

        orderRepository.findById(saved.getId());
    }

    @Test
    @DisplayName("AOP logs OrderRepository.findAll() call")
    void shouldLogRepositoryFindAll() {
        orderRepository.save(newOrder("mouse"));

        orderRepository.findAll();
    }

    @Test
    @DisplayName("AOP logs OrderRepository.deleteAll() call")
    void shouldLogRepositoryDeleteAll() {
        orderRepository.save(newOrder("keyboard"));

        orderRepository.deleteAll();
    }

    private Order newOrder(String productName) {
        Order order = new Order();
        order.setProductName(productName);
        order.setQuantity(1);
        order.setPrice(new BigDecimal("100.00"));
        order.setStatus(OrderStatus.PRICED);
        return order;
    }
}
