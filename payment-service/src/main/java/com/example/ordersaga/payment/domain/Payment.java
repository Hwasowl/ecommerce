package com.example.ordersaga.payment.domain;

import com.example.ordersaga.payment.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String paymentId;

    @Column(nullable = false, length = 50)
    private String orderId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 30)
    private String pgProvider;

    @Column(length = 100)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 100)
    private String orderName;

    @Column(length = 100)
    private String pgTransactionId;

    private LocalDateTime approvedAt;

    private LocalDateTime canceledAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PaymentItemSnapshot> itemSnapshots = new ArrayList<>();

    @Builder
    private Payment(
        String paymentId,
        String orderId,
        Long customerId,
        String pgProvider,
        String paymentKey,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String orderName,
        String pgTransactionId
    ) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.pgProvider = pgProvider;
        this.paymentKey = paymentKey;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.orderName = orderName;
        this.pgTransactionId = pgTransactionId;
    }

    public static Payment pending(
        String paymentId,
        String orderId,
        Long customerId,
        BigDecimal amount,
        String currency,
        String orderName
    ) {
        return Payment.builder()
            .paymentId(paymentId)
            .orderId(orderId)
            .customerId(customerId)
            .pgProvider("TOSS_PAYMENTS")
            .status(PaymentStatus.PENDING)
            .amount(amount)
            .currency(currency)
            .orderName(orderName)
            .build();
    }

    public void addItemSnapshot(PaymentItemSnapshot itemSnapshot) {
        itemSnapshots.add(itemSnapshot);
        itemSnapshot.assignPayment(this);
    }

    public void confirm(String paymentKey, String pgTransactionId) {
        this.paymentKey = paymentKey;
        this.pgTransactionId = pgTransactionId;
        this.status = PaymentStatus.CONFIRMED;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }
}
