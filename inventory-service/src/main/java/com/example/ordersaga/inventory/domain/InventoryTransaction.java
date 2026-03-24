package com.example.ordersaga.inventory.domain;

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
@Table(name = "inventory_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String transactionId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 50)
    private String orderId;

    @Column(nullable = false, length = 50)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryTransactionType transactionType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer beforeStock;

    @Column(nullable = false)
    private Integer afterStock;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private InventoryTransaction(
        String transactionId,
        Long productId,
        String orderId,
        String paymentId,
        InventoryTransactionType transactionType,
        Integer quantity,
        Integer beforeStock,
        Integer afterStock,
        String reason,
        LocalDateTime createdAt
    ) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.beforeStock = beforeStock;
        this.afterStock = afterStock;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public static InventoryTransaction of(
        String transactionId,
        Long productId,
        String orderId,
        String paymentId,
        InventoryTransactionType transactionType,
        Integer quantity,
        Integer beforeStock,
        Integer afterStock,
        String reason
    ) {
        return InventoryTransaction.builder()
            .transactionId(transactionId)
            .productId(productId)
            .orderId(orderId)
            .paymentId(paymentId)
            .transactionType(transactionType)
            .quantity(quantity)
            .beforeStock(beforeStock)
            .afterStock(afterStock)
            .reason(reason)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
