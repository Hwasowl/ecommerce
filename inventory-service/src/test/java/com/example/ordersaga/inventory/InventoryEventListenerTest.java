package com.example.ordersaga.inventory;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordersaga.inventory.application.InventoryProcessingResult;
import com.example.ordersaga.inventory.application.InventoryService;
import com.example.ordersaga.inventory.application.dto.InventoryFailedEvent;
import com.example.ordersaga.inventory.application.dto.InventoryReservedEvent;
import com.example.ordersaga.inventory.application.dto.PaymentConfirmedEvent;
import com.example.ordersaga.inventory.infrastructure.kafka.InventoryEventListener;
import com.example.ordersaga.inventory.infrastructure.kafka.InventoryEventPublisher;
import com.example.ordersaga.inventory.infrastructure.kafka.InventoryKafkaTopicsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryEventListenerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryEventPublisher inventoryEventPublisher;

    private final InventoryKafkaTopicsProperties topicsProperties =
        new InventoryKafkaTopicsProperties("payment.confirmed", "inventory.reserved", "inventory.failed", "inventory-service-group");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("재고 예약 성공이면 InventoryReserved를 발행한다.")
    void publishInventoryReservedWhenReservationSucceeds() throws Exception {
        InventoryEventListener listener = new InventoryEventListener(
            inventoryService,
            inventoryEventPublisher,
            topicsProperties,
            objectMapper
        );
        PaymentConfirmedEvent event = paymentConfirmedEvent();
        InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
            "evt-inv-res-1",
            "InventoryReserved",
            LocalDateTime.now(),
            event.orderId(),
            event.paymentId(),
            event.eventId(),
            "RES-1",
            List.of(new InventoryReservedEvent.ReservedItem(101L, 2))
        );
        when(inventoryService.handlePaymentConfirmed(event))
            .thenReturn(InventoryProcessingResult.reserved(reservedEvent));

        listener.consumePaymentConfirmed(objectMapper.writeValueAsString(event));

        verify(inventoryEventPublisher).publishInventoryReserved(reservedEvent);
        verify(inventoryEventPublisher, never()).publishInventoryFailed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("재고 예약 실패면 InventoryFailed를 발행한다.")
    void publishInventoryFailedWhenReservationFails() throws Exception {
        InventoryEventListener listener = new InventoryEventListener(
            inventoryService,
            inventoryEventPublisher,
            topicsProperties,
            objectMapper
        );
        PaymentConfirmedEvent event = paymentConfirmedEvent();
        InventoryFailedEvent failedEvent = new InventoryFailedEvent(
            "evt-inv-fail-1",
            "InventoryFailed",
            LocalDateTime.now(),
            event.orderId(),
            event.paymentId(),
            event.eventId(),
            "INSUFFICIENT_STOCK",
            "재고가 부족합니다.",
            List.of(new InventoryFailedEvent.FailedItem(101L, 2))
        );
        when(inventoryService.handlePaymentConfirmed(event))
            .thenReturn(InventoryProcessingResult.failed(failedEvent));

        listener.consumePaymentConfirmed(objectMapper.writeValueAsString(event));

        verify(inventoryEventPublisher).publishInventoryFailed(failedEvent);
        verify(inventoryEventPublisher, never()).publishInventoryReserved(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("중복 이벤트면 후속 이벤트를 발행하지 않는다.")
    void skipPublishingWhenEventIsDuplicated() throws Exception {
        InventoryEventListener listener = new InventoryEventListener(
            inventoryService,
            inventoryEventPublisher,
            topicsProperties,
            objectMapper
        );
        PaymentConfirmedEvent event = paymentConfirmedEvent();
        when(inventoryService.handlePaymentConfirmed(event))
            .thenReturn(InventoryProcessingResult.duplicated());

        listener.consumePaymentConfirmed(objectMapper.writeValueAsString(event));

        verify(inventoryEventPublisher, never()).publishInventoryReserved(org.mockito.ArgumentMatchers.any());
        verify(inventoryEventPublisher, never()).publishInventoryFailed(org.mockito.ArgumentMatchers.any());
    }

    private PaymentConfirmedEvent paymentConfirmedEvent() {
        return new PaymentConfirmedEvent(
            "evt-pay-1",
            "PaymentConfirmed",
            LocalDateTime.now(),
            "ORD-1001",
            "PAY-1001",
            "payment-key-1",
            1L,
            BigDecimal.valueOf(130000),
            "KRW",
            "무선 키보드 외 1건",
            List.of(new PaymentConfirmedEvent.PaymentConfirmedItem(
                101L,
                "keyboard",
                2,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(100000)
            ))
        );
    }
}
