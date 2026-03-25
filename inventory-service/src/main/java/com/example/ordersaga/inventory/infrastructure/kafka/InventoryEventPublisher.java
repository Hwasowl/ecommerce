package com.example.ordersaga.inventory.infrastructure.kafka;

import com.example.ordersaga.inventory.application.dto.InventoryFailedEvent;
import com.example.ordersaga.inventory.application.dto.InventoryReservedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final InventoryKafkaTopicsProperties topicsProperties;
    private final ObjectMapper objectMapper;

    public void publishInventoryReserved(InventoryReservedEvent event) {
        kafkaTemplate.send(topicsProperties.inventoryReserved(), event.orderId(), toJson(event)).join();
    }

    public void publishInventoryFailed(InventoryFailedEvent event) {
        kafkaTemplate.send(topicsProperties.inventoryFailed(), event.orderId(), toJson(event)).join();
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("인벤토리 이벤트 페이로드 직렬화에 실패했습니다.", ex);
        }
    }
}
