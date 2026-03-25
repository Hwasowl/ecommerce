package com.example.ordersaga.inventory.application;

import com.example.ordersaga.inventory.application.dto.InventoryFailedEvent;
import com.example.ordersaga.inventory.application.dto.InventoryReservedEvent;

public record InventoryProcessingResult(
    boolean duplicate,
    InventoryReservedEvent inventoryReservedEvent,
    InventoryFailedEvent inventoryFailedEvent
) {
    public static InventoryProcessingResult duplicated() {
        return new InventoryProcessingResult(true, null, null);
    }

    public static InventoryProcessingResult reserved(InventoryReservedEvent event) {
        return new InventoryProcessingResult(false, event, null);
    }

    public static InventoryProcessingResult failed(InventoryFailedEvent event) {
        return new InventoryProcessingResult(false, null, event);
    }
}
