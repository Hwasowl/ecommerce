package com.example.ordersaga.inventory.domain;

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
@Table(name = "inventory_inbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 50)
    private String aggregateId;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InboxProcessStatus processStatus;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;

    @Builder
    private InventoryInbox(
        String eventId,
        String eventType,
        String aggregateId,
        String payloadJson,
        InboxProcessStatus processStatus,
        LocalDateTime receivedAt
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payloadJson = payloadJson;
        this.processStatus = processStatus;
        this.receivedAt = receivedAt;
    }

    public static InventoryInbox received(String eventId, String eventType, String aggregateId, String payloadJson) {
        return InventoryInbox.builder()
            .eventId(eventId)
            .eventType(eventType)
            .aggregateId(aggregateId)
            .payloadJson(payloadJson)
            .processStatus(InboxProcessStatus.RECEIVED)
            .receivedAt(LocalDateTime.now())
            .build();
    }

    public void markProcessed() {
        this.processStatus = InboxProcessStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.processStatus = InboxProcessStatus.FAILED;
        this.processedAt = LocalDateTime.now();
    }
}
