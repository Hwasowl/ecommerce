package com.example.ordersaga.payment.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCompensatedEvent(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    String orderId,
    String paymentId,
    String sourceEventId,
    String compensationId,
    String compensationType,
    BigDecimal amount,
    String currency,
    String reason
) {
}
