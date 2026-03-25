package com.example.ordersaga.payment.infrastructure.kafka;

import com.example.ordersaga.payment.application.dto.PaymentCompensatedEvent;
import com.example.ordersaga.payment.domain.PaymentOutbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentKafkaTopicsProperties topicsProperties;
    private final ObjectMapper objectMapper;

    public void publish(PaymentOutbox outbox) {
        kafkaTemplate.send(resolveTopic(outbox.getEventType()), outbox.getAggregateId(), outbox.getPayloadJson()).join();
    }

    public void publishPaymentCompensated(PaymentCompensatedEvent event) {
        kafkaTemplate.send(topicsProperties.paymentCompensated(), event.orderId(), toJson(event)).join();
    }

    private String resolveTopic(String eventType) {
        if ("PaymentConfirmed".equals(eventType)) {
            return topicsProperties.paymentConfirmed();
        }
        throw new IllegalArgumentException("지원하지 않는 payment eventType 입니다. eventType=" + eventType);
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("결제 이벤트 페이로드 직렬화에 실패했습니다.", ex);
        }
    }
}
