package com.example.ordersaga.payment.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InventoryFailedEvent(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    String orderId,
    String paymentId,
    String sourceEventId,
    String failureCode,
    String failureReason,
    List<FailedItem> items
) {
    public record FailedItem(
        Long productId,
        Integer requestedQuantity
    ) {
    }
}
