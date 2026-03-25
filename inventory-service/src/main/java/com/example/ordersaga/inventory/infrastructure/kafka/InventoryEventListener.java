package com.example.ordersaga.inventory.infrastructure.kafka;

import com.example.ordersaga.inventory.application.InventoryProcessingResult;
import com.example.ordersaga.inventory.application.InventoryService;
import com.example.ordersaga.inventory.application.dto.PaymentConfirmedEvent;
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
public class InventoryEventListener {

    private final InventoryService inventoryService;
    private final InventoryEventPublisher inventoryEventPublisher;
    private final InventoryKafkaTopicsProperties topicsProperties;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${inventory.kafka.topics.payment-confirmed}",
        groupId = "${inventory.kafka.topics.inventory-group-id}"
    )
    public void consumePaymentConfirmed(@Payload String payload) {
        PaymentConfirmedEvent event = fromJson(payload);
        InventoryProcessingResult result = inventoryService.handlePaymentConfirmed(event);

        if (result.duplicate()) {
            log.info("중복 결제 확정 이벤트를 무시했습니다. eventId={}", event.eventId());
            return;
        }
        if (result.inventoryReservedEvent() != null) {
            inventoryEventPublisher.publishInventoryReserved(result.inventoryReservedEvent());
            log.info("재고 예약 성공 이벤트를 발행했습니다. orderId={}, paymentId={}", event.orderId(), event.paymentId());
            return;
        }

        inventoryEventPublisher.publishInventoryFailed(result.inventoryFailedEvent());
        log.info("재고 예약 실패 이벤트를 발행했습니다. orderId={}, paymentId={}", event.orderId(), event.paymentId());
    }

    private PaymentConfirmedEvent fromJson(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentConfirmedEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("결제 확정 이벤트 페이로드 역직렬화에 실패했습니다.", ex);
        }
    }
}
