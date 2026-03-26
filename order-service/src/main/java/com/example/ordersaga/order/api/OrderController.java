package com.example.ordersaga.order.api;

import com.example.ordersaga.order.application.OrderService;
import com.example.ordersaga.order.application.dto.CreateOrderRequest;
import com.example.ordersaga.order.application.dto.CreateOrderResponse;
import com.example.ordersaga.order.application.dto.OrderStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "주문 API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "주문 생성", description = "주문과 주문 상품을 생성하고 초기 상태를 CREATED로 저장합니다.")
    public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "주문 상태 조회", description = "orderId 기준으로 현재 주문 상태와 기본 정보를 반환합니다.")
    public OrderStatusResponse getOrderStatus(@PathVariable String orderId) {
        return orderService.getOrderStatus(orderId);
    }
}

