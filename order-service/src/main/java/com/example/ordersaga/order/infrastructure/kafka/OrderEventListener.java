package com.example.ordersaga.order.infrastructure.kafka;

import com.example.ordersaga.order.application.OrderService;
import com.example.ordersaga.order.application.dto.InventoryReservedEvent;
import com.example.ordersaga.order.application.dto.PaymentCompensatedEvent;
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
public class OrderEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${order.kafka.topics.inventory-reserved}",
        groupId = "${order.kafka.topics.order-group-id}"
    )
    public void consumeInventoryReserved(@Payload String payload) {
        InventoryReservedEvent event = fromJson(payload, InventoryReservedEvent.class);
        orderService.handleInventoryReserved(event);
        log.info("주문 성공 상태 전이를 처리했습니다. orderId={}, eventId={}", event.orderId(), event.eventId());
    }

    @KafkaListener(
        topics = "${order.kafka.topics.payment-compensated}",
        groupId = "${order.kafka.topics.order-group-id}"
    )
    public void consumePaymentCompensated(@Payload String payload) {
        PaymentCompensatedEvent event = fromJson(payload, PaymentCompensatedEvent.class);
        orderService.handlePaymentCompensated(event);
        log.info("주문 실패 상태 전이를 처리했습니다. orderId={}, eventId={}", event.orderId(), event.eventId());
    }

    private <T> T fromJson(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("주문 이벤트 페이로드 역직렬화에 실패했습니다.", ex);
        }
    }
}
