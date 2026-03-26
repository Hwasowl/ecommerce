package com.example.ordersaga.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ordersaga.payment.application.PaymentService;
import com.example.ordersaga.payment.application.dto.CreatePaymentItemRequest;
import com.example.ordersaga.payment.application.dto.CreatePaymentRequest;
import com.example.ordersaga.payment.application.dto.CreatePaymentResponse;
import com.example.ordersaga.payment.application.dto.PaymentStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@Transactional
class PaymentStatusQueryIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 상태 조회 시 생성된 결제의 현재 상태를 반환한다.")
    void getPaymentStatus() {
        CreatePaymentResponse created = paymentService.createPayment(
            new CreatePaymentRequest(
                "ORD-9001",
                11L,
                BigDecimal.valueOf(159000),
                "KRW",
                "gaming keyboard",
                List.of(new CreatePaymentItemRequest(9001L, "gaming keyboard", 1, BigDecimal.valueOf(159000)))
            )
        );

        PaymentStatusResponse result = paymentService.getPaymentStatus(created.paymentId());

        assertThat(result.paymentId()).isEqualTo(created.paymentId());
        assertThat(result.orderId()).isEqualTo("ORD-9001");
        assertThat(result.customerId()).isEqualTo(11L);
        assertThat(result.status()).isEqualTo(created.status());
        assertThat(result.amount()).isEqualByComparingTo("159000");
        assertThat(result.pgProvider()).isEqualTo("TOSS_PAYMENTS");
    }
}
