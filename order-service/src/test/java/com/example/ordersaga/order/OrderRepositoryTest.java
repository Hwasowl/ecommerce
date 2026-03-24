package com.example.ordersaga.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ordersaga.order.domain.Order;
import com.example.ordersaga.order.repository.OrderRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("orderId로 주문을 조회할 수 있다.")
    void findOrderByOrderId() {
        orderRepository.save(Order.create("ORD-REPO-1", 1L, BigDecimal.valueOf(10000), "KRW"));

        assertThat(orderRepository.findByOrderId("ORD-REPO-1")).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 orderId로 조회하면 결과가 없다.")
    void returnEmptyWhenOrderIdDoesNotExist() {
        assertThat(orderRepository.findByOrderId("ORD-NOT-FOUND")).isEmpty();
    }

    @Test
    @DisplayName("주문을 저장하면 ID가 생성된다.")
    void generateIdWhenOrderIsSaved() {
        Order order = orderRepository.save(Order.create("ORD-REPO-2", 2L, BigDecimal.valueOf(20000), "KRW"));

        assertThat(order.getId()).isNotNull();
    }
}
