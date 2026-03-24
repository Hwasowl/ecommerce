package com.example.ordersaga.order.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
    @NotNull Long customerId,
    @NotBlank String currency,
    @Valid @NotEmpty List<CreateOrderItemRequest> items
) {
}

