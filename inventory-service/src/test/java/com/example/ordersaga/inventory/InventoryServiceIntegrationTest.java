package com.example.ordersaga.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordersaga.inventory.application.InventoryProcessingResult;
import com.example.ordersaga.inventory.application.InventoryService;
import com.example.ordersaga.inventory.application.InventoryStockStore;
import com.example.ordersaga.inventory.application.dto.PaymentConfirmedEvent;
import com.example.ordersaga.inventory.domain.InboxProcessStatus;
import com.example.ordersaga.inventory.domain.Inventory;
import com.example.ordersaga.inventory.repository.InventoryInboxRepository;
import com.example.ordersaga.inventory.repository.InventoryRepository;
import com.example.ordersaga.inventory.repository.InventoryReservationRepository;
import com.example.ordersaga.inventory.repository.InventoryTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class InventoryServiceIntegrationTest {

    private static final long KEYBOARD_PRODUCT_ID = 5101L;
    private static final long MOUSE_PRODUCT_ID = 5102L;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryInboxRepository inventoryInboxRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @MockBean
    private InventoryStockStore inventoryStockStore;

    @BeforeEach
    void setUp() {
        inventoryTransactionRepository.deleteAll();
        inventoryReservationRepository.deleteAll();
        inventoryInboxRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    @Test
    @DisplayName("결제 확정 이벤트를 처리하면 재고 예약 결과와 이력을 저장한다.")
    void handlePaymentConfirmedSuccessfully() {
        inventoryRepository.save(Inventory.of(KEYBOARD_PRODUCT_ID, 10));
        inventoryRepository.save(Inventory.of(MOUSE_PRODUCT_ID, 5));

        when(inventoryStockStore.reserve(anyList())).thenReturn(
            InventoryStockStore.ReservationAttempt.success(List.of(
                new InventoryStockStore.ReservedStock(KEYBOARD_PRODUCT_ID, 2, 10, 8),
                new InventoryStockStore.ReservedStock(MOUSE_PRODUCT_ID, 1, 5, 4)
            ))
        );

        InventoryProcessingResult result = inventoryService.handlePaymentConfirmed(paymentConfirmedEvent("event-1"));

        assertThat(result.duplicate()).isFalse();
        assertThat(result.inventoryReservedEvent()).isNotNull();
        assertThat(result.inventoryFailedEvent()).isNull();
        assertThat(result.inventoryReservedEvent().items()).hasSize(2);
        assertThat(inventoryInboxRepository.findAll()).hasSize(1);
        assertThat(inventoryInboxRepository.findAll().get(0).getProcessStatus()).isEqualTo(InboxProcessStatus.PROCESSED);
        assertThat(inventoryReservationRepository.findAll()).hasSize(2);
        assertThat(inventoryTransactionRepository.findAll()).hasSize(2);
        assertThat(inventoryRepository.findByProductId(KEYBOARD_PRODUCT_ID).orElseThrow().getAvailableStock()).isEqualTo(8);
        assertThat(inventoryRepository.findByProductId(MOUSE_PRODUCT_ID).orElseThrow().getAvailableStock()).isEqualTo(4);
        verify(inventoryStockStore).reserve(anyList());
    }

    @Test
    @DisplayName("재고 예약에 실패하면 실패 이벤트를 반환하고 예약 이력을 남기지 않는다.")
    void handlePaymentConfirmedFailure() {
        inventoryRepository.save(Inventory.of(KEYBOARD_PRODUCT_ID, 1));

        when(inventoryStockStore.reserve(anyList())).thenReturn(
            InventoryStockStore.ReservationAttempt.failure("INSUFFICIENT_STOCK", "재고가 부족합니다.")
        );

        InventoryProcessingResult result = inventoryService.handlePaymentConfirmed(paymentConfirmedEvent("event-2"));

        assertThat(result.duplicate()).isFalse();
        assertThat(result.inventoryReservedEvent()).isNull();
        assertThat(result.inventoryFailedEvent()).isNotNull();
        assertThat(result.inventoryFailedEvent().failureCode()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(inventoryInboxRepository.findAll()).hasSize(1);
        assertThat(inventoryInboxRepository.findAll().get(0).getProcessStatus()).isEqualTo(InboxProcessStatus.FAILED);
        assertThat(inventoryReservationRepository.findAll()).isEmpty();
        assertThat(inventoryTransactionRepository.findAll()).isEmpty();
        assertThat(inventoryRepository.findByProductId(KEYBOARD_PRODUCT_ID).orElseThrow().getAvailableStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 eventId를 다시 처리하면 중복으로 판단하고 재고 저장소를 다시 호출하지 않는다.")
    void ignoreDuplicateEvent() {
        inventoryRepository.save(Inventory.of(KEYBOARD_PRODUCT_ID, 10));
        when(inventoryStockStore.reserve(anyList())).thenReturn(
            InventoryStockStore.ReservationAttempt.success(List.of(
                new InventoryStockStore.ReservedStock(KEYBOARD_PRODUCT_ID, 2, 10, 8)
            ))
        );

        PaymentConfirmedEvent event = paymentConfirmedEvent("event-dup");

        inventoryService.handlePaymentConfirmed(event);
        InventoryProcessingResult duplicateResult = inventoryService.handlePaymentConfirmed(event);

        assertThat(duplicateResult.duplicate()).isTrue();
        assertThat(inventoryInboxRepository.findAll()).hasSize(1);
        assertThat(inventoryReservationRepository.findAll()).hasSize(1);
        verify(inventoryStockStore).reserve(anyList());
    }

    private PaymentConfirmedEvent paymentConfirmedEvent(String eventId) {
        return new PaymentConfirmedEvent(
            eventId,
            "PaymentConfirmed",
            LocalDateTime.now(),
            "ORD-1001",
            "PAY-1001",
            "payment-key-1",
            1L,
            BigDecimal.valueOf(130000),
            "KRW",
            "무선 키보드 외 1건",
            List.of(
                new PaymentConfirmedEvent.PaymentConfirmedItem(KEYBOARD_PRODUCT_ID, "keyboard", 2, BigDecimal.valueOf(50000), BigDecimal.valueOf(100000)),
                new PaymentConfirmedEvent.PaymentConfirmedItem(MOUSE_PRODUCT_ID, "mouse", 1, BigDecimal.valueOf(30000), BigDecimal.valueOf(30000))
            )
        );
    }
}
