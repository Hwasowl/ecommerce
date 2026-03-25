package com.example.ordersaga.order;

import static org.mockito.Mockito.verify;

import com.example.ordersaga.order.application.OrderService;
import com.example.ordersaga.order.application.dto.InventoryReservedEvent;
import com.example.ordersaga.order.application.dto.PaymentCompensatedEvent;
import com.example.ordersaga.order.infrastructure.kafka.OrderEventListener;
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
class OrderEventListenerTest {

    @Mock
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("InventoryReserved 메시지를 받으면 주문 서비스에 성공 상태 전이를 위임한다.")
    void delegateInventoryReserved() throws Exception {
        OrderEventListener listener = new OrderEventListener(orderService, objectMapper);
        InventoryReservedEvent event = new InventoryReservedEvent(
            "evt-inv-res-1",
            "InventoryReserved",
            LocalDateTime.now(),
            "ORD-1001",
            "PAY-1001",
            "evt-pay-1",
            "RES-1001",
            List.of(new InventoryReservedEvent.ReservedItem(101L, 2))
        );

        listener.consumeInventoryReserved(objectMapper.writeValueAsString(event));

        verify(orderService).handleInventoryReserved(event);
    }

    @Test
    @DisplayName("PaymentCompensated 메시지를 받으면 주문 서비스에 실패 상태 전이를 위임한다.")
    void delegatePaymentCompensated() throws Exception {
        OrderEventListener listener = new OrderEventListener(orderService, objectMapper);
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
            "재고 확보 실패로 결제를 취소했습니다."
        );

        listener.consumePaymentCompensated(objectMapper.writeValueAsString(event));

        verify(orderService).handlePaymentCompensated(event);
    }
}
