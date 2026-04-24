package com.example.order;

import com.example.order.client.PricingClient;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Epic("Allure instrumentation")
@Feature("Mockito — stub / call / verify")
class MockitoLoggingTest extends BaseIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private PricingClient pricingClient;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Mockito logs the stub phase — when(...).thenReturn(...)")
    void shouldLogMockStubPhase() {
        when(pricingClient.getPrice("laptop")).thenReturn(new BigDecimal("999.99"));
    }

    @Test
    @DisplayName("Mockito logs the call phase — production code invoking the mock")
    void shouldLogMockCallPhase() {
        when(pricingClient.getPrice("speaker")).thenReturn(new BigDecimal("45.00"));

        orderService.createOrder(new CreateOrderRequest("speaker", 2));
    }

    @Test
    @DisplayName("Mockito logs the verify phase — verify(mock).method(...)")
    void shouldLogMockVerifyPhase() {
        when(pricingClient.getPrice("tablet")).thenReturn(new BigDecimal("100.00"));
        orderService.createOrder(new CreateOrderRequest("tablet", 1));

        verify(pricingClient, times(1)).getPrice(eq("tablet"));
    }
}
