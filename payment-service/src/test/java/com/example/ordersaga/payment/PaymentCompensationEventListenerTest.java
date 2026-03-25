package com.example.ordersaga.payment;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordersaga.payment.application.PaymentCompensationService;
import com.example.ordersaga.payment.application.dto.InventoryFailedEvent;
import com.example.ordersaga.payment.application.dto.PaymentCompensatedEvent;
import com.example.ordersaga.payment.infrastructure.kafka.PaymentCompensationEventListener;
import com.example.ordersaga.payment.infrastructure.kafka.PaymentEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentCompensationEventListenerTest {

    @Mock
    private PaymentCompensationService paymentCompensationService;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("재고 실패 이벤트를 받으면 보상 완료 이벤트를 발행한다.")
    void publishPaymentCompensatedWhenCompensationSucceeds() throws Exception {
        PaymentCompensationEventListener listener = new PaymentCompensationEventListener(
            paymentCompensationService,
            paymentEventPublisher,
            objectMapper
        );
        InventoryFailedEvent failedEvent = inventoryFailedEvent("evt-inv-fail-1");
        PaymentCompensatedEvent compensatedEvent = new PaymentCompensatedEvent(
            "evt-pay-comp-1",
            "PaymentCompensated",
            LocalDateTime.now(),
            failedEvent.orderId(),
            failedEvent.paymentId(),
            failedEvent.eventId(),
            "COMP-1",
            "CANCEL",
            BigDecimal.valueOf(130000),
            "KRW",
            "재고가 부족합니다."
        );
        when(paymentCompensationService.handleInventoryFailed(failedEvent)).thenReturn(compensatedEvent);

        listener.consumeInventoryFailed(objectMapper.writeValueAsString(failedEvent));

        verify(paymentEventPublisher).publishPaymentCompensated(compensatedEvent);
    }

    @Test
    @DisplayName("중복 재고 실패 이벤트면 보상 완료 이벤트를 발행하지 않는다.")
    void skipPublishingWhenInventoryFailedIsDuplicated() throws Exception {
        PaymentCompensationEventListener listener = new PaymentCompensationEventListener(
            paymentCompensationService,
            paymentEventPublisher,
            objectMapper
        );
        InventoryFailedEvent failedEvent = inventoryFailedEvent("evt-inv-fail-dup");
        when(paymentCompensationService.handleInventoryFailed(failedEvent)).thenReturn(null);

        listener.consumeInventoryFailed(objectMapper.writeValueAsString(failedEvent));

        verify(paymentEventPublisher, never()).publishPaymentCompensated(org.mockito.ArgumentMatchers.any());
    }

    private InventoryFailedEvent inventoryFailedEvent(String eventId) {
        return new InventoryFailedEvent(
            eventId,
            "InventoryFailed",
            LocalDateTime.now(),
            "ORD-5001",
            "PAY-5001",
            "evt-pay-5001",
            "INSUFFICIENT_STOCK",
            "재고가 부족합니다.",
            List.of(new InventoryFailedEvent.FailedItem(101L, 2))
        );
    }
}
