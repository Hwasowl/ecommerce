package com.example.ordersaga.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordersaga.payment.application.PaymentCompensationService;
import com.example.ordersaga.payment.application.dto.InventoryFailedEvent;
import com.example.ordersaga.payment.application.dto.PaymentCompensatedEvent;
import com.example.ordersaga.payment.domain.CompensationStatus;
import com.example.ordersaga.payment.domain.Payment;
import com.example.ordersaga.payment.domain.PaymentCompensation;
import com.example.ordersaga.payment.domain.PaymentStatus;
import com.example.ordersaga.payment.infrastructure.toss.TossPaymentCancelClient;
import com.example.ordersaga.payment.repository.PaymentCompensationRepository;
import com.example.ordersaga.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PaymentCompensationServiceIntegrationTest {

    @Autowired
    private PaymentCompensationService paymentCompensationService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentCompensationRepository paymentCompensationRepository;

    @MockBean
    private TossPaymentCancelClient tossPaymentCancelClient;

    @Test
    @DisplayName("재고 실패 이벤트를 처리하면 결제를 취소하고 PaymentCompensated를 반환한다.")
    void compensatePaymentWhenInventoryFailed() {
        paymentRepository.save(Payment.pending("PAY-5001", "ORD-5001", 1L, BigDecimal.valueOf(130000), "KRW", "무선 키보드"));
        Payment payment = paymentRepository.findByPaymentId("PAY-5001").orElseThrow();
        payment.confirm("payment-key-1", "payment-key-1");

        PaymentCompensatedEvent result = paymentCompensationService.handleInventoryFailed(inventoryFailedEvent("evt-inv-fail-1"));

        assertThat(result).isNotNull();
        assertThat(result.eventType()).isEqualTo("PaymentCompensated");
        assertThat(paymentRepository.findByPaymentId("PAY-5001").orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELED);
        PaymentCompensation compensation = paymentCompensationRepository.findBySourceEventId("evt-inv-fail-1").orElseThrow();
        assertThat(compensation.getStatus()).isEqualTo(CompensationStatus.COMPLETED);
        verify(tossPaymentCancelClient).cancel(payment, "재고가 부족합니다.");
    }

    @Test
    @DisplayName("같은 InventoryFailed 이벤트를 다시 처리하면 보상을 중복 수행하지 않는다.")
    void ignoreDuplicateInventoryFailedEvent() {
        paymentRepository.save(Payment.pending("PAY-5002", "ORD-5002", 1L, BigDecimal.valueOf(90000), "KRW", "유선 마우스"));
        paymentCompensationRepository.save(PaymentCompensation.requested(
            "COMP-5002",
            "PAY-5002",
            "ORD-5002",
            "evt-inv-fail-dup",
            "CANCEL",
            BigDecimal.valueOf(90000),
            "재고가 부족합니다."
        ));

        PaymentCompensatedEvent result = paymentCompensationService.handleInventoryFailed(inventoryFailedEvent("evt-inv-fail-dup"));

        assertThat(result).isNull();
    }

    private InventoryFailedEvent inventoryFailedEvent(String eventId) {
        return new InventoryFailedEvent(
            eventId,
            "InventoryFailed",
            LocalDateTime.now(),
            "ORD-5001",
            "PAY-5001",
            "evt-pay-5001",
            "INSUFFICIENT_STOCK",
            "재고가 부족합니다.",
            List.of(new InventoryFailedEvent.FailedItem(101L, 2))
        );
    }
}
