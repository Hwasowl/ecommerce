package com.example.ordersaga.inventory.application;

import com.example.ordersaga.inventory.application.dto.InventoryFailedEvent;
import com.example.ordersaga.inventory.application.dto.InventoryReservedEvent;
import com.example.ordersaga.inventory.application.dto.PaymentConfirmedEvent;
import com.example.ordersaga.inventory.domain.Inventory;
import com.example.ordersaga.inventory.domain.InventoryInbox;
import com.example.ordersaga.inventory.domain.InventoryReservation;
import com.example.ordersaga.inventory.domain.InventoryTransaction;
import com.example.ordersaga.inventory.domain.InventoryTransactionType;
import com.example.ordersaga.inventory.repository.InventoryInboxRepository;
import com.example.ordersaga.inventory.repository.InventoryRepository;
import com.example.ordersaga.inventory.repository.InventoryReservationRepository;
import com.example.ordersaga.inventory.repository.InventoryTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryInboxRepository inventoryInboxRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryStockStore inventoryStockStore;
    private final ObjectMapper objectMapper;

    @Transactional
    public InventoryProcessingResult handlePaymentConfirmed(PaymentConfirmedEvent event) {
        if (inventoryInboxRepository.findByEventId(event.eventId()).isPresent()) {
            return InventoryProcessingResult.duplicated();
        }

        InventoryInbox inbox = inventoryInboxRepository.save(InventoryInbox.received(
            event.eventId(),
            event.eventType(),
            event.orderId(),
            toPayload(event)
        ));

        InventoryStockStore.ReservationAttempt reservationAttempt = inventoryStockStore.reserve(
            event.items().stream()
                .map(item -> new InventoryStockStore.ReserveCommand(item.productId(), item.quantity()))
                .toList()
        );

        if (!reservationAttempt.success()) {
            inbox.markFailed();
            return InventoryProcessingResult.failed(toInventoryFailedEvent(event, reservationAttempt));
        }

        String reservationGroupId = generateId("RES");

        for (InventoryStockStore.ReservedStock reservedStock : reservationAttempt.reservedStocks()) {
            Inventory inventory = inventoryRepository.findByProductId(reservedStock.productId())
                .orElseThrow(() -> new IllegalStateException(
                    "재고 엔티티를 찾을 수 없습니다. productId=" + reservedStock.productId()
                ));

            inventory.reserve(reservedStock.quantity());
            inventoryReservationRepository.save(InventoryReservation.reserve(
                reservationGroupId + "-" + reservedStock.productId(),
                event.orderId(),
                event.paymentId(),
                reservedStock.productId(),
                reservedStock.quantity()
            ));
            inventoryTransactionRepository.save(InventoryTransaction.of(
                generateId("ITX"),
                reservedStock.productId(),
                event.orderId(),
                event.paymentId(),
                InventoryTransactionType.RESERVE,
                reservedStock.quantity(),
                reservedStock.beforeStock(),
                reservedStock.afterStock(),
                "결제 확정 이벤트 기반 재고 예약"
            ));
        }

        inbox.markProcessed();
        return InventoryProcessingResult.reserved(toInventoryReservedEvent(event, reservationGroupId));
    }

    private InventoryReservedEvent toInventoryReservedEvent(PaymentConfirmedEvent event, String reservationGroupId) {
        return new InventoryReservedEvent(
            generateId("EVT-INV-RES"),
            "InventoryReserved",
            LocalDateTime.now(),
            event.orderId(),
            event.paymentId(),
            event.eventId(),
            reservationGroupId,
            event.items().stream()
                .map(item -> new InventoryReservedEvent.ReservedItem(item.productId(), item.quantity()))
                .toList()
        );
    }

    private InventoryFailedEvent toInventoryFailedEvent(
        PaymentConfirmedEvent event,
        InventoryStockStore.ReservationAttempt reservationAttempt
    ) {
        return new InventoryFailedEvent(
            generateId("EVT-INV-FAIL"),
            "InventoryFailed",
            LocalDateTime.now(),
            event.orderId(),
            event.paymentId(),
            event.eventId(),
            reservationAttempt.failureCode(),
            reservationAttempt.failureReason(),
            event.items().stream()
                .map(item -> new InventoryFailedEvent.FailedItem(item.productId(), item.quantity()))
                .toList()
        );
    }

    private String toPayload(PaymentConfirmedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("결제 확정 이벤트 페이로드 직렬화에 실패했습니다.", ex);
        }
    }

    private String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
