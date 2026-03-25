package com.example.ordersaga.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payment_item_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentItemSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id_fk", nullable = false)
    private Payment payment;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineAmount;

    @Builder
    private PaymentItemSnapshot(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount
    ) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineAmount = lineAmount;
    }

    public static PaymentItemSnapshot of(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount
    ) {
        return PaymentItemSnapshot.builder()
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .lineAmount(lineAmount)
            .build();
    }

    public void assignPayment(Payment payment) {
        this.payment = payment;
    }
}
