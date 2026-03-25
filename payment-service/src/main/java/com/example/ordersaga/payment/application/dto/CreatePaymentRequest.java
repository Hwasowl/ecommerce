package com.example.ordersaga.payment.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CreatePaymentRequest(
    @NotBlank String orderId,
    @NotNull Long customerId,
    @NotNull @Min(0) BigDecimal amount,
    @NotBlank String currency,
    @NotBlank String orderName,
    @NotNull List<@jakarta.validation.Valid CreatePaymentItemRequest> items
) {
}

