package com.example.order;

import com.example.order.client.PricingClient;
import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import io.qameta.allure.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Epic("Order Service")
@Feature("MockMvc + Mockito + JPA + Kafka")
class OrderMockMvcMockitoTest extends BaseIntegrationTest {

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
    @Story("Create Order")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/orders — создание заказа через MockMvc + Mockito, проверка в БД")
    void shouldCreateOrderAndPersistToDb() throws Exception {
        when(pricingClient.getPrice("laptop")).thenReturn(new BigDecimal("999.99"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productName": "laptop", "quantity": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.productName").value("laptop"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.price").value(1999.98))
                .andExpect(jsonPath("$.status").value("PRICED"));

        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getProductName()).isEqualTo("laptop");
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PRICED);
    }

    @Test
    @Story("Get Order")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("GET /api/orders/{id} — получение заказа через MockMvc")
    void shouldReturnOrderById() throws Exception {
        Order order = new Order();
        order.setProductName("phone");
        order.setQuantity(1);
        order.setPrice(new BigDecimal("599.00"));
        order.setStatus(OrderStatus.PRICED);
        Order saved = orderRepository.save(order);

        mockMvc.perform(get("/api/orders/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.productName").value("phone"))
                .andExpect(jsonPath("$.price").value(599.00));
    }

    @Test
    @Story("Get Order")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /api/orders/{id} — 404 для несуществующего заказа через MockMvc")
    void shouldReturn404ForNonExistentOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 99999))
                .andExpect(status().isNotFound());
    }

    @Test
    @Story("Create Order")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/orders — 400 при невалидном запросе через MockMvc")
    void shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productName": "", "quantity": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Story("Kafka Integration")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/orders — отправка события в Kafka через MockMvc + Mockito")
    void shouldSendKafkaEventOnOrderCreation() throws Exception {
        when(pricingClient.getPrice(anyString())).thenReturn(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productName": "tablet", "quantity": 1}
                                """))
                .andExpect(status().isCreated());

        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of("order-events"));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            assertThat(records.iterator().next().value()).contains("\"status\":\"CREATED\"");
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-group",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
    }
}
