package com.example.ordersaga.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ordersaga.payment.application.TossWebhookService;
import com.example.ordersaga.payment.application.dto.TossWebhookRequest;
import com.example.ordersaga.payment.domain.Payment;
import com.example.ordersaga.payment.domain.PaymentItemSnapshot;
import com.example.ordersaga.payment.domain.PaymentStatus;
import com.example.ordersaga.payment.exception.BusinessException;
import com.example.ordersaga.payment.repository.PaymentOutboxRepository;
import com.example.ordersaga.payment.repository.PaymentRepository;
import com.example.ordersaga.payment.repository.PaymentWebhookRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TossWebhookServiceIntegrationTest {

    @Autowired
    private TossWebhookService tossWebhookService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentWebhookRepository paymentWebhookRepository;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("결제 성공 웹훅을 처리하면 결제가 확정되고 웹훅과 아웃박스가 저장된다.")
    void confirmPaymentAndCreateWebhookAndOutboxWhenWebhookIsReceived() throws Exception {
        Payment payment = Payment.pending("PAY-1001", "ORD-1001", 1L, BigDecimal.valueOf(130000), "KRW", "무선 키보드 외 1건");
        payment.addItemSnapshot(PaymentItemSnapshot.of(101L, "keyboard", 2, BigDecimal.valueOf(50000), BigDecimal.valueOf(100000)));
        payment.addItemSnapshot(PaymentItemSnapshot.of(102L, "mouse", 1, BigDecimal.valueOf(30000), BigDecimal.valueOf(30000)));
        paymentRepository.save(payment);

        TossWebhookRequest request = new TossWebhookRequest(
            "event-1",
            "PAYMENT_STATUS_CHANGED",
            "PAY-1001",
            "ORD-1001",
            "payment-key-1",
            "DONE",
            "{\"status\":\"DONE\"}"
        );

        tossWebhookService.handle("signature-1", request);

        Payment confirmedPayment = paymentRepository.findByPaymentId("PAY-1001").orElseThrow();
        assertThat(confirmedPayment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(paymentWebhookRepository.findByEventId("event-1")).isPresent();
        String payloadJson = paymentOutboxRepository.findByEventId("event-1").orElseThrow().getPayloadJson();
        JsonNode payload = objectMapper.readTree(payloadJson);
        assertThat(payload.get("eventType").asText()).isEqualTo("PaymentConfirmed");
        assertThat(payload.get("orderId").asText()).isEqualTo("ORD-1001");
        assertThat(payload.get("customerId").asLong()).isEqualTo(1L);
        assertThat(payload.get("items")).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 결제의 웹훅이 오면 예외가 발생한다.")
    void failWebhookHandlingWhenPaymentDoesNotExist() {
        TossWebhookRequest request = new TossWebhookRequest(
            "event-missing",
            "PAYMENT_STATUS_CHANGED",
            "PAY-9999",
            "ORD-9999",
            "payment-key-missing",
            "DONE",
            "{\"status\":\"DONE\"}"
        );

        assertThatThrownBy(() -> tossWebhookService.handle("signature-1", request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("결제 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("같은 eventId의 웹훅이 다시 오면 중복 저장하지 않는다.")
    void ignoreDuplicateWebhookWhenSameEventIsReceived() {
        Payment payment = Payment.pending("PAY-2001", "ORD-2001", 2L, BigDecimal.valueOf(50000), "KRW", "유선 마우스");
        payment.addItemSnapshot(PaymentItemSnapshot.of(201L, "mouse", 1, BigDecimal.valueOf(50000), BigDecimal.valueOf(50000)));
        paymentRepository.save(payment);

        TossWebhookRequest request = new TossWebhookRequest(
            "event-dup",
            "PAYMENT_STATUS_CHANGED",
            "PAY-2001",
            "ORD-2001",
            "payment-key-2",
            "DONE",
            "{\"status\":\"DONE\"}"
        );

        tossWebhookService.handle("signature-1", request);
        tossWebhookService.handle("signature-1", request);

        assertThat(paymentWebhookRepository.findAll()).hasSize(1);
        assertThat(paymentOutboxRepository.findAll()).hasSize(1);
    }
}
