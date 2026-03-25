package com.example.ordersaga.inventory.application;

import java.util.List;

public interface InventoryStockStore {

    ReservationAttempt reserve(List<ReserveCommand> commands);

    record ReserveCommand(
        Long productId,
        Integer quantity
    ) {
    }

    record ReservedStock(
        Long productId,
        Integer quantity,
        Integer beforeStock,
        Integer afterStock
    ) {
    }

    record ReservationAttempt(
        boolean success,
        String failureCode,
        String failureReason,
        List<ReservedStock> reservedStocks
    ) {
        public static ReservationAttempt success(List<ReservedStock> reservedStocks) {
            return new ReservationAttempt(true, null, null, reservedStocks);
        }

        public static ReservationAttempt failure(String failureCode, String failureReason) {
            return new ReservationAttempt(false, failureCode, failureReason, List.of());
        }
    }
}
