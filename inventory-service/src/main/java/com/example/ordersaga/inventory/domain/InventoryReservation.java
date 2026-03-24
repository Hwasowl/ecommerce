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
@Table(name = "inventory_reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String reservationId;

    @Column(nullable = false, length = 50)
    private String orderId;

    @Column(nullable = false, length = 50)
    private String paymentId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    private LocalDateTime releasedAt;

    @Builder
    private InventoryReservation(
        String reservationId,
        String orderId,
        String paymentId,
        Long productId,
        Integer quantity,
        ReservationStatus status,
        LocalDateTime reservedAt
    ) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.reservedAt = reservedAt;
    }

    public static InventoryReservation reserve(
        String reservationId,
        String orderId,
        String paymentId,
        Long productId,
        Integer quantity
    ) {
        return InventoryReservation.builder()
            .reservationId(reservationId)
            .orderId(orderId)
            .paymentId(paymentId)
            .productId(productId)
            .quantity(quantity)
            .status(ReservationStatus.RESERVED)
            .reservedAt(LocalDateTime.now())
            .build();
    }

    public void release() {
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = ReservationStatus.FAILED;
    }
}
