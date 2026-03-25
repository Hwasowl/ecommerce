package com.example.ordersaga.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory.redis")
public record InventoryRedisProperties(
    String stockKeyPrefix
) {
}
