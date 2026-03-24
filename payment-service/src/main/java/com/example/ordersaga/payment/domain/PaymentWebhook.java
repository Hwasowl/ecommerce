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
@Table(name = "payment_webhooks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String paymentId;

    @Column(nullable = false, length = 50)
    private String orderId;

    @Column(length = 100)
    private String paymentKey;

    @Column(nullable = false, length = 50)
    private String webhookType;

    @Column(nullable = false, length = 30)
    private String webhookStatus;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Column(length = 255)
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebhookProcessStatus processStatus;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;

    @Builder
    private PaymentWebhook(
        String eventId,
        String paymentId,
        String orderId,
        String paymentKey,
        String webhookType,
        String webhookStatus,
        String payloadJson,
        String signature,
        WebhookProcessStatus processStatus,
        LocalDateTime receivedAt
    ) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.webhookType = webhookType;
        this.webhookStatus = webhookStatus;
        this.payloadJson = payloadJson;
        this.signature = signature;
        this.processStatus = processStatus;
        this.receivedAt = receivedAt;
    }

    public static PaymentWebhook received(
        String eventId,
        String paymentId,
        String orderId,
        String paymentKey,
        String webhookType,
        String webhookStatus,
        String payloadJson,
        String signature
    ) {
        return PaymentWebhook.builder()
            .eventId(eventId)
            .paymentId(paymentId)
            .orderId(orderId)
            .paymentKey(paymentKey)
            .webhookType(webhookType)
            .webhookStatus(webhookStatus)
            .payloadJson(payloadJson)
            .signature(signature)
            .processStatus(WebhookProcessStatus.RECEIVED)
            .receivedAt(LocalDateTime.now())
            .build();
    }

    public void markProcessing() {
        this.processStatus = WebhookProcessStatus.PROCESSING;
    }

    public void markCompleted() {
        this.processStatus = WebhookProcessStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void markIgnored() {
        this.processStatus = WebhookProcessStatus.IGNORED;
        this.processedAt = LocalDateTime.now();
    }
}
