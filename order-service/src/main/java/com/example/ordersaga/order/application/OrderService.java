package com.example.ordersaga.order.application;

import com.example.ordersaga.order.application.dto.CreateOrderItemRequest;
import com.example.ordersaga.order.application.dto.CreateOrderRequest;
import com.example.ordersaga.order.application.dto.CreateOrderResponse;
import com.example.ordersaga.order.domain.Order;
import com.example.ordersaga.order.domain.OrderItem;
import com.example.ordersaga.order.domain.OrderStatusHistory;
import com.example.ordersaga.order.repository.OrderRepository;
import com.example.ordersaga.order.repository.OrderStatusHistoryRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        BigDecimal totalAmount = request.items().stream()
            .map(this::calculateLineAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.create(orderId, request.customerId(), totalAmount, request.currency());
        request.items().forEach(item -> order.addItem(toOrderItem(item)));

        orderRepository.save(order);
        orderStatusHistoryRepository.save(OrderStatusHistory.of(orderId, null, order.getStatus(), "ORDER_CREATED", null));

        return new CreateOrderResponse(order.getOrderId(), order.getStatus(), order.getTotalAmount(), order.getCurrency());
    }

    private OrderItem toOrderItem(CreateOrderItemRequest item) {
        return OrderItem.builder()
            .productId(item.productId())
            .productName(item.productName())
            .quantity(item.quantity())
            .unitPrice(item.unitPrice())
            .lineAmount(calculateLineAmount(item))
            .build();
    }

    private BigDecimal calculateLineAmount(CreateOrderItemRequest item) {
        return item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
    }
}

