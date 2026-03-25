package com.example.ordersaga.payment;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordersaga.payment.application.dto.PaymentCompensatedEvent;
import com.example.ordersaga.payment.infrastructure.kafka.PaymentEventPublisher;
import com.example.ordersaga.payment.infrastructure.kafka.PaymentKafkaTopicsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("PaymentCompensated는 payment.compensated 토픽으로 발행한다.")
    void publishPaymentCompensated() throws Exception {
        PaymentKafkaTopicsProperties properties =
            new PaymentKafkaTopicsProperties("payment.confirmed", "inventory.failed", "payment.compensated", "payment-service-group");
        PaymentEventPublisher publisher = new PaymentEventPublisher(kafkaTemplate, properties, objectMapper);
        PaymentCompensatedEvent event = new PaymentCompensatedEvent(
            "evt-pay-comp-1",
            "PaymentCompensated",
            LocalDateTime.now(),
            "ORD-1001",
            "PAY-1001",
            "evt-inv-fail-1",
            "COMP-1001",
            "CANCEL",
            BigDecimal.valueOf(130000),
            "KRW",
            "재고가 부족합니다."
        );
        String payload = objectMapper.writeValueAsString(event);
        when(kafkaTemplate.send("payment.compensated", "ORD-1001", payload))
            .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPaymentCompensated(event);

        verify(kafkaTemplate).send("payment.compensated", "ORD-1001", payload);
    }
}
