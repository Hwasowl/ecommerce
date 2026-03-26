package com.example.ordersaga.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ordersaga.order.application.OrderService;
import com.example.ordersaga.order.application.dto.CreateOrderItemRequest;
import com.example.ordersaga.order.application.dto.CreateOrderRequest;
import com.example.ordersaga.order.application.dto.CreateOrderResponse;
import com.example.ordersaga.order.application.dto.OrderStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@Transactional
class OrderStatusQueryIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    @DisplayName("주문 상태 조회 시 생성된 주문의 현재 상태를 반환한다.")
    void getOrderStatus() {
        CreateOrderResponse created = orderService.createOrder(
            new CreateOrderRequest(
                7L,
                "KRW",
                List.of(new CreateOrderItemRequest(501L, "headset", 1, BigDecimal.valueOf(89000)))
            )
        );

        OrderStatusResponse result = orderService.getOrderStatus(created.orderId());

        assertThat(result.orderId()).isEqualTo(created.orderId());
        assertThat(result.customerId()).isEqualTo(7L);
        assertThat(result.status()).isEqualTo(created.status());
        assertThat(result.totalAmount()).isEqualByComparingTo("89000");
    }
}
