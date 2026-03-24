package com.example.ordersaga.payment.application.dto;

import com.example.ordersaga.payment.domain.PaymentStatus;
import java.math.BigDecimal;

public record CreatePaymentResponse(
    String paymentId,
    String orderId,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String clientKey,
    String successUrl,
    String failUrl
) {
}

