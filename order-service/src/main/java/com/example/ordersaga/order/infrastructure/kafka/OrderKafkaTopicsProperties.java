package com.example.ordersaga.order.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.kafka.topics")
public record OrderKafkaTopicsProperties(
    String inventoryReserved,
    String paymentCompensated,
    String orderGroupId
) {
}
