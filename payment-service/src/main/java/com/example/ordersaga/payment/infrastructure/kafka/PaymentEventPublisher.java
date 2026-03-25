package com.example.ordersaga.payment.infrastructure.kafka;

import com.example.ordersaga.payment.domain.PaymentOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentKafkaTopicsProperties topicsProperties;

    public void publish(PaymentOutbox outbox) {
        kafkaTemplate.send(resolveTopic(outbox.getEventType()), outbox.getAggregateId(), outbox.getPayloadJson()).join();
    }

    private String resolveTopic(String eventType) {
        if ("PaymentConfirmed".equals(eventType)) {
            return topicsProperties.paymentConfirmed();
        }
        throw new IllegalArgumentException("지원하지 않는 payment eventType 입니다. eventType=" + eventType);
    }
}
