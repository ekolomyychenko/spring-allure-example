package com.example.order.service;

import com.example.order.client.PricingClient;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PricingClient pricingClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderService(OrderRepository orderRepository,
                        PricingClient pricingClient,
                        KafkaTemplate<String, String> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.pricingClient = pricingClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        BigDecimal price = pricingClient.getPrice(request.productName());

        Order order = new Order();
        order.setProductName(request.productName());
        order.setQuantity(request.quantity());
        order.setPrice(price.multiply(BigDecimal.valueOf(request.quantity())));
        order.setStatus(OrderStatus.PRICED);

        Order saved = orderRepository.save(order);

        kafkaTemplate.send("order-events", String.valueOf(saved.getId()),
                "{\"orderId\":" + saved.getId() + ",\"status\":\"CREATED\"}");

        return saved;
    }

    public Optional<Order> getOrder(Long id) {
        return orderRepository.findById(id);
    }
}
