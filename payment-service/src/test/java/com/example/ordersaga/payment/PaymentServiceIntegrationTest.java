package com.example.ordersaga.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ordersaga.payment.application.PaymentService;
import com.example.ordersaga.payment.application.dto.CreatePaymentItemRequest;
import com.example.ordersaga.payment.application.dto.CreatePaymentRequest;
import com.example.ordersaga.payment.application.dto.CreatePaymentResponse;
import com.example.ordersaga.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("결제를 생성하면 PENDING 상태로 저장된다.")
    void createPaymentAndPersistPendingStatus() {
        CreatePaymentResponse response = paymentService.createPayment(
            new CreatePaymentRequest(
                "ORD-1001",
                1L,
                BigDecimal.valueOf(130000),
                "KRW",
                "무선 키보드",
                List.of(new CreatePaymentItemRequest(101L, "keyboard", 2, BigDecimal.valueOf(65000)))
            )
        );

        assertThat(response.paymentId()).startsWith("PAY-");
        assertThat(response.status().name()).isEqualTo("PENDING");
        assertThat(paymentRepository.findByPaymentId(response.paymentId())).isPresent();
    }

    @Test
    @DisplayName("결제를 생성하면 PG 제공자는 TOSS_PAYMENTS로 저장된다.")
    void createPaymentWithTossPaymentsProvider() {
        CreatePaymentResponse response = paymentService.createPayment(
            new CreatePaymentRequest(
                "ORD-2001",
                2L,
                BigDecimal.valueOf(50000),
                "KRW",
                "유선 마우스",
                List.of(new CreatePaymentItemRequest(201L, "mouse", 1, BigDecimal.valueOf(50000)))
            )
        );

        assertThat(paymentRepository.findByPaymentId(response.paymentId())).isPresent();
        assertThat(paymentRepository.findByPaymentId(response.paymentId()).orElseThrow().getPgProvider())
            .isEqualTo("TOSS_PAYMENTS");
    }
}
