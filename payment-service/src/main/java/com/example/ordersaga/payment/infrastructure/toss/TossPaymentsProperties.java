package com.example.ordersaga.payment.infrastructure.toss;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.toss-payments")
public record TossPaymentsProperties(
    String baseUrl,
    String clientKey,
    String secretKey,
    String webhookSecret,
    String successUrl,
    String failUrl
) {
}
