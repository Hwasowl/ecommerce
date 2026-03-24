package com.example.ordersaga.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_status_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus toStatus;

    @Column(length = 100)
    private String reason;

    @Column(length = 100)
    private String eventId;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @Builder
    private OrderStatusHistory(
        String orderId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String reason,
        String eventId,
        LocalDateTime changedAt
    ) {
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.eventId = eventId;
        this.changedAt = changedAt;
    }

    public static OrderStatusHistory of(
        String orderId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String reason,
        String eventId
    ) {
        return OrderStatusHistory.builder()
            .orderId(orderId)
            .fromStatus(fromStatus)
            .toStatus(toStatus)
            .reason(reason)
            .eventId(eventId)
            .changedAt(LocalDateTime.now())
            .build();
    }
}

