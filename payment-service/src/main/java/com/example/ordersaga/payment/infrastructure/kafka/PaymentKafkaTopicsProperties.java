package com.example.ordersaga.payment.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.kafka.topics")
public record PaymentKafkaTopicsProperties(
    String paymentConfirmed,
    String inventoryFailed,
    String paymentCompensated,
    String paymentGroupId
) {
}