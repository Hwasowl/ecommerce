package com.example.ordersaga.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payment_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String aggregateType;

    @Column(nullable = false, length = 50)
    private String aggregateId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxStatus publishStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @Builder
    private PaymentOutbox(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payloadJson,
        OutboxStatus publishStatus,
        LocalDateTime createdAt
    ) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.publishStatus = publishStatus;
        this.createdAt = createdAt;
    }

    public static PaymentOutbox init(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payloadJson
    ) {
        return PaymentOutbox.builder()
            .eventId(eventId)
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(eventType)
            .payloadJson(payloadJson)
            .publishStatus(OutboxStatus.INIT)
            .createdAt(LocalDateTime.now())
            .build();
    }

    public void markPublished() {
        this.publishStatus = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.publishStatus = OutboxStatus.FAILED;
    }
}
