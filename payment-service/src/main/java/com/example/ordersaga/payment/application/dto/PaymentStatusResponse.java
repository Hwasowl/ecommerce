package com.example.ordersaga.payment.application.dto;

import com.example.ordersaga.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentStatusResponse(
    String paymentId,
    String orderId,
    Long customerId,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String orderName,
    String paymentKey,
    String pgProvider,
    LocalDateTime approvedAt,
    LocalDateTime canceledAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
