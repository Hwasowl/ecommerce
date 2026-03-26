package com.example.ordersaga.order.application.dto;

import com.example.ordersaga.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderStatusResponse(
    String orderId,
    Long customerId,
    OrderStatus status,
    BigDecimal totalAmount,
    String currency,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
