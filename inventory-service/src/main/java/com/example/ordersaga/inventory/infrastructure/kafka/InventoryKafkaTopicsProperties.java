package com.example.ordersaga.inventory.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory.kafka.topics")
public record InventoryKafkaTopicsProperties(
    String paymentConfirmed,
    String inventoryReserved,
    String inventoryFailed,
    String inventoryGroupId
) {
}
