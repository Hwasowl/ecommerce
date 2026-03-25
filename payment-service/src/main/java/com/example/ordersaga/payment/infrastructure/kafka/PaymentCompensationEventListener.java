package com.example.ordersaga.payment.infrastructure.kafka;

import com.example.ordersaga.payment.application.PaymentCompensationService;
import com.example.ordersaga.payment.application.dto.InventoryFailedEvent;
import com.example.ordersaga.payment.application.dto.PaymentCompensatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompensationEventListener {

    private final PaymentCompensationService paymentCompensationService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${payment.kafka.topics.inventory-failed}",
        groupId = "${payment.kafka.topics.payment-group-id}"
    )
    public void consumeInventoryFailed(@Payload String payload) {
        InventoryFailedEvent event = fromJson(payload);
        PaymentCompensatedEvent compensatedEvent = paymentCompensationService.handleInventoryFailed(event);
        if (compensatedEvent == null) {
            log.info("중복 재고 실패 이벤트를 무시했습니다. eventId={}", event.eventId());
            return;
        }

        paymentEventPublisher.publishPaymentCompensated(compensatedEvent);
        log.info("결제 보상 완료 이벤트를 발행했습니다. orderId={}, paymentId={}", event.orderId(), event.paymentId());
    }

    private InventoryFailedEvent fromJson(String payload) {
        try {
            return objectMapper.readValue(payload, InventoryFailedEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("재고 실패 이벤트 페이로드 역직렬화에 실패했습니다.", ex);
        }
    }
}
