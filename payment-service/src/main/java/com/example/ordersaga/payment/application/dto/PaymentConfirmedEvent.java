package com.example.ordersaga.payment.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentConfirmedEvent(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    String orderId,
    String paymentId,
    String paymentKey,
    Long customerId,
    BigDecimal amount,
    String currency,
    String orderName,
    List<PaymentConfirmedItem> items
) {
    public record PaymentConfirmedItem(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount
    ) {
    }
}
