package com.example.ordersaga.order.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InventoryReservedEvent(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    String orderId,
    String paymentId,
    String sourceEventId,
    String reservationId,
    List<ReservedItem> items
) {
    public record ReservedItem(
        Long productId,
        Integer quantity
    ) {
    }
}
