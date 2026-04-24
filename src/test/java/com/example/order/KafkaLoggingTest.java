package com.example.order;

import com.example.order.client.PricingClient;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Epic("Allure instrumentation")
@Feature("Kafka — KafkaConsumer.poll() via ByteBuddy")
class KafkaLoggingTest extends BaseIntegrationTest {

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
    @DisplayName("ByteBuddy logs KafkaConsumer.poll() result after producer send")
    void shouldLogKafkaPoll() {
        when(pricingClient.getPrice(anyString())).thenReturn(new BigDecimal("100.00"));

        orderService.createOrder(new CreateOrderRequest("tablet", 1));

        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of("order-events"));
            consumer.poll(Duration.ofSeconds(10));
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
