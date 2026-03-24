package com.example.ordersaga.payment.application.dto;

import jakarta.validation.constraints.NotBlank;

public record TossWebhookRequest(
    @NotBlank String eventId,
    @NotBlank String eventType,
    @NotBlank String paymentId,
    @NotBlank String orderId,
    @NotBlank String paymentKey,
    @NotBlank String status,
    String rawPayload
) {
}
