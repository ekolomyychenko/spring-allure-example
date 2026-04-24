package com.example.order;

import com.example.order.client.PricingClient;
import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Epic("Allure instrumentation")
@Feature("MockMvc — HTTP request/response logging")
class MockMvcLoggingTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private PricingClient pricingClient;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("MockMvc logs POST request body and 201 response")
    void shouldLogMockMvcPost201() throws Exception {
        when(pricingClient.getPrice("laptop")).thenReturn(new BigDecimal("999.99"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productName": "laptop", "quantity": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PRICED"));
    }

    @Test
    @DisplayName("MockMvc logs GET request and 200 response")
    void shouldLogMockMvcGet200() throws Exception {
        Order order = new Order();
        order.setProductName("phone");
        order.setQuantity(1);
        order.setPrice(new BigDecimal("599.00"));
        order.setStatus(OrderStatus.PRICED);
        Order saved = orderRepository.save(order);

        mockMvc.perform(get("/api/orders/{id}", saved.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("MockMvc logs GET request and 404 response")
    void shouldLogMockMvcGet404() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 99999))
                .andExpect(status().isNotFound());
    }
}
