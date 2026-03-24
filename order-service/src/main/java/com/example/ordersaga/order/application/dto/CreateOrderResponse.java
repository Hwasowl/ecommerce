package com.example.ordersaga.order.application.dto;

import com.example.ordersaga.order.domain.OrderStatus;
import java.math.BigDecimal;

public record CreateOrderResponse(
    String orderId,
    OrderStatus status,
    BigDecimal totalAmount,
    String currency
) {
}

