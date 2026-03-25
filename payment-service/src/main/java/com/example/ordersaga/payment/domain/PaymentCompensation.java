package com.example.ordersaga.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payment_compensations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCompensation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String compensationId;

    @Column(nullable = false, length = 50)
    private String paymentId;

    @Column(nullable = false, length = 50)
    private String orderId;

    @Column(nullable = false, unique = true, length = 100)
    private String sourceEventId;

    @Column(nullable = false, length = 30)
    private String compensationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompensationStatus status;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    @Builder
    private PaymentCompensation(
        String compensationId,
        String paymentId,
        String orderId,
        String sourceEventId,
        String compensationType,
        CompensationStatus status,
        BigDecimal amount,
        String reason,
        LocalDateTime requestedAt
    ) {
        this.compensationId = compensationId;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.sourceEventId = sourceEventId;
        this.compensationType = compensationType;
        this.status = status;
        this.amount = amount;
        this.reason = reason;
        this.requestedAt = requestedAt;
    }

    public static PaymentCompensation requested(
        String compensationId,
        String paymentId,
        String orderId,
        String sourceEventId,
        String compensationType,
        BigDecimal amount,
        String reason
    ) {
        return PaymentCompensation.builder()
            .compensationId(compensationId)
            .paymentId(paymentId)
            .orderId(orderId)
            .sourceEventId(sourceEventId)
            .compensationType(compensationType)
            .status(CompensationStatus.REQUESTED)
            .amount(amount)
            .reason(reason)
            .requestedAt(LocalDateTime.now())
            .build();
    }

    public void markCompleted() {
        this.status = CompensationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = CompensationStatus.FAILED;
    }
}
