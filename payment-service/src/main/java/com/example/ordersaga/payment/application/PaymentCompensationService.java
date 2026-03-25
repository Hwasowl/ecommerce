package com.example.ordersaga.payment.application;

import com.example.ordersaga.payment.application.dto.InventoryFailedEvent;
import com.example.ordersaga.payment.application.dto.PaymentCompensatedEvent;
import com.example.ordersaga.payment.domain.Payment;
import com.example.ordersaga.payment.domain.PaymentCompensation;
import com.example.ordersaga.payment.exception.BusinessException;
import com.example.ordersaga.payment.exception.ErrorCode;
import com.example.ordersaga.payment.infrastructure.toss.TossPaymentCancelClient;
import com.example.ordersaga.payment.repository.PaymentCompensationRepository;
import com.example.ordersaga.payment.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCompensationService {

    private final PaymentRepository paymentRepository;
    private final PaymentCompensationRepository paymentCompensationRepository;
    private final TossPaymentCancelClient tossPaymentCancelClient;

    @Transactional
    public PaymentCompensatedEvent handleInventoryFailed(InventoryFailedEvent event) {
        if (paymentCompensationRepository.findBySourceEventId(event.eventId()).isPresent()) {
            return null;
        }

        Payment payment = paymentRepository.findByPaymentId(event.paymentId())
            .orElseThrow(() -> new BusinessException(
                ErrorCode.PAYMENT_NOT_FOUND,
                "결제 정보를 찾을 수 없습니다. paymentId=" + event.paymentId()
            ));

        PaymentCompensation compensation = paymentCompensationRepository.save(PaymentCompensation.requested(
            generateId("COMP"),
            payment.getPaymentId(),
            payment.getOrderId(),
            event.eventId(),
            "CANCEL",
            payment.getAmount(),
            event.failureReason()
        ));

        try {
            tossPaymentCancelClient.cancel(payment, event.failureReason());
            payment.cancel();
            compensation.markCompleted();
            return new PaymentCompensatedEvent(
                generateId("EVT-PAY-COMP"),
                "PaymentCompensated",
                LocalDateTime.now(),
                payment.getOrderId(),
                payment.getPaymentId(),
                event.eventId(),
                compensation.getCompensationId(),
                compensation.getCompensationType(),
                payment.getAmount(),
                payment.getCurrency(),
                event.failureReason()
            );
        } catch (RuntimeException ex) {
            compensation.markFailed();
            throw ex;
        }
    }

    private String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
