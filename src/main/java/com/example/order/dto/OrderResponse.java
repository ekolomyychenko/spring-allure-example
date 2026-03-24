package com.example.order.dto;

import com.example.order.model.Order;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        String productName,
        int quantity,
        BigDecimal price,
        String status,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductName(),
                order.getQuantity(),
                order.getPrice(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
}
