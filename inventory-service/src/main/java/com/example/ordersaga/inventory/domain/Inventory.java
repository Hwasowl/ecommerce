package com.example.ordersaga.inventory.domain;

import com.example.ordersaga.inventory.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "inventories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private Integer totalStock;

    @Column(nullable = false)
    private Integer reservedStock;

    @Column(nullable = false)
    private Integer availableStock;

    @Builder
    private Inventory(Long productId, Integer totalStock, Integer reservedStock, Integer availableStock) {
        this.productId = productId;
        this.totalStock = totalStock;
        this.reservedStock = reservedStock;
        this.availableStock = availableStock;
    }

    public static Inventory of(Long productId, Integer totalStock) {
        return Inventory.builder()
            .productId(productId)
            .totalStock(totalStock)
            .reservedStock(0)
            .availableStock(totalStock)
            .build();
    }

    public void reserve(int quantity) {
        if (availableStock < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }
        this.availableStock -= quantity;
        this.reservedStock += quantity;
    }

    public void release(int quantity) {
        this.availableStock += quantity;
        this.reservedStock -= quantity;
    }
}
