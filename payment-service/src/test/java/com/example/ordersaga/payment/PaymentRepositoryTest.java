package com.example.ordersaga.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ordersaga.payment.domain.Payment;
import com.example.ordersaga.payment.domain.PaymentWebhook;
import com.example.ordersaga.payment.repository.PaymentRepository;
import com.example.ordersaga.payment.repository.PaymentWebhookRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentWebhookRepository paymentWebhookRepository;

    @Test
    @DisplayName("paymentId로 결제를 조회할 수 있다.")
    void findPaymentByPaymentId() {
        paymentRepository.save(Payment.pending("PAY-REPO-1", "ORD-REPO-1", 1L, BigDecimal.valueOf(10000), "KRW", "테스트 주문"));

        assertThat(paymentRepository.findByPaymentId("PAY-REPO-1")).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 paymentId로 조회하면 결과가 없다.")
    void returnEmptyWhenPaymentIdDoesNotExist() {
        assertThat(paymentRepository.findByPaymentId("PAY-NOT-FOUND")).isEmpty();
    }

    @Test
    @DisplayName("eventId로 웹훅을 조회할 수 있다.")
    void findWebhookByEventId() {
        paymentWebhookRepository.save(PaymentWebhook.received(
            "event-repo-1",
            "PAY-REPO-1",
            "ORD-REPO-1",
            "payment-key-1",
            "PAYMENT_STATUS_CHANGED",
            "DONE",
            "{\"status\":\"DONE\"}",
            "signature-1"
        ));

        assertThat(paymentWebhookRepository.findByEventId("event-repo-1")).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 eventId로 웹훅을 조회하면 결과가 없다.")
    void returnEmptyWhenWebhookEventIdDoesNotExist() {
        assertThat(paymentWebhookRepository.findByEventId("event-not-found")).isEmpty();
    }
}
