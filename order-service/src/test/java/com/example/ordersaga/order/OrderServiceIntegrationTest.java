package com.example.ordersaga.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ordersaga.order.application.OrderService;
import com.example.ordersaga.order.application.dto.CreateOrderItemRequest;
import com.example.ordersaga.order.application.dto.CreateOrderRequest;
import com.example.ordersaga.order.application.dto.CreateOrderResponse;
import com.example.ordersaga.order.domain.OrderStatus;
import com.example.ordersaga.order.repository.OrderRepository;
import com.example.ordersaga.order.repository.OrderStatusHistoryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Test
    @DisplayName("주문을 생성하면 주문 상품과 주문 상태 이력이 함께 저장된다.")
    void createOrderAndPersistItemsAndStatusHistory() {
        CreateOrderRequest request = new CreateOrderRequest(
            1L,
            "KRW",
            List.of(
                new CreateOrderItemRequest(101L, "keyboard", 2, BigDecimal.valueOf(50000)),
                new CreateOrderItemRequest(102L, "mouse", 1, BigDecimal.valueOf(30000))
            )
        );

        CreateOrderResponse response = orderService.createOrder(request);

        assertThat(response.orderId()).startsWith("ORD-");
        assertThat(response.totalAmount()).isEqualByComparingTo("130000");
        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(orderRepository.findAll().get(0).getItems()).hasSize(2);
        assertThat(orderStatusHistoryRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("주문을 생성하면 초기 상태는 CREATED다.")
    void createOrderWithCreatedStatus() {
        CreateOrderResponse response = orderService.createOrder(
            new CreateOrderRequest(
                2L,
                "KRW",
                List.of(new CreateOrderItemRequest(201L, "monitor", 1, BigDecimal.valueOf(200000)))
            )
        );

        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
    }
}
