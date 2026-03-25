package com.example.ordersaga.inventory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordersaga.inventory.application.dto.InventoryFailedEvent;
import com.example.ordersaga.inventory.application.dto.InventoryReservedEvent;
import com.example.ordersaga.inventory.infrastructure.kafka.InventoryEventPublisher;
import com.example.ordersaga.inventory.infrastructure.kafka.InventoryKafkaTopicsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class InventoryEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final InventoryKafkaTopicsProperties topicsProperties =
        new InventoryKafkaTopicsProperties("payment.confirmed", "inventory.reserved", "inventory.failed", "inventory-service-group");

    @Test
    @DisplayName("InventoryReserved는 inventory.reserved 토픽으로 발행한다.")
    void publishInventoryReserved() throws Exception {
        InventoryEventPublisher publisher = new InventoryEventPublisher(kafkaTemplate, topicsProperties, objectMapper);
        InventoryReservedEvent event = new InventoryReservedEvent(
            "evt-inv-res-1",
            "InventoryReserved",
            LocalDateTime.now(),
            "ORD-1001",
            "PAY-1001",
            "evt-pay-1",
            "RES-1",
            List.of(new InventoryReservedEvent.ReservedItem(101L, 2))
        );
        String payload = objectMapper.writeValueAsString(event);
        when(kafkaTemplate.send("inventory.reserved", "ORD-1001", payload))
            .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishInventoryReserved(event);

        verify(kafkaTemplate).send("inventory.reserved", "ORD-1001", payload);
    }

    @Test
    @DisplayName("InventoryFailed는 inventory.failed 토픽으로 발행한다.")
    void publishInventoryFailed() throws Exception {
        InventoryEventPublisher publisher = new InventoryEventPublisher(kafkaTemplate, topicsProperties, objectMapper);
        InventoryFailedEvent event = new InventoryFailedEvent(
            "evt-inv-fail-1",
            "InventoryFailed",
            LocalDateTime.now(),
            "ORD-1001",
            "PAY-1001",
            "evt-pay-1",
            "INSUFFICIENT_STOCK",
            "재고가 부족합니다.",
            List.of(new InventoryFailedEvent.FailedItem(101L, 2))
        );
        String payload = objectMapper.writeValueAsString(event);
        when(kafkaTemplate.send("inventory.failed", "ORD-1001", payload))
            .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishInventoryFailed(event);

        verify(kafkaTemplate).send("inventory.failed", "ORD-1001", payload);
    }
}
