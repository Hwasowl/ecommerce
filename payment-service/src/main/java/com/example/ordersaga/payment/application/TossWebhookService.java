package com.example.ordersaga.payment.application;

import com.example.ordersaga.payment.application.dto.TossWebhookRequest;
import com.example.ordersaga.payment.domain.Payment;
import com.example.ordersaga.payment.domain.PaymentOutbox;
import com.example.ordersaga.payment.domain.PaymentWebhook;
import com.example.ordersaga.payment.exception.BusinessException;
import com.example.ordersaga.payment.exception.ErrorCode;
import com.example.ordersaga.payment.repository.PaymentOutboxRepository;
import com.example.ordersaga.payment.repository.PaymentRepository;
import com.example.ordersaga.payment.repository.PaymentWebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TossWebhookService {

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookRepository paymentWebhookRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;

    @Transactional
    public void handle(String signature, TossWebhookRequest request) {
        if (paymentWebhookRepository.findByEventId(request.eventId()).isPresent()) {
            return;
        }

        Payment payment = paymentRepository.findByPaymentId(request.paymentId())
            .orElseThrow(() -> new BusinessException(
                ErrorCode.PAYMENT_NOT_FOUND,
                "결제 정보를 찾을 수 없습니다. paymentId=" + request.paymentId()
            ));

        PaymentWebhook webhook = PaymentWebhook.received(
            request.eventId(),
            request.paymentId(),
            request.orderId(),
            request.paymentKey(),
            request.eventType(),
            request.status(),
            request.rawPayload(),
            signature
        );

        webhook.markProcessing();
        payment.confirm(request.paymentKey(), request.paymentKey());
        paymentOutboxRepository.save(PaymentOutbox.init(
            request.eventId(),
            "PAYMENT",
            payment.getPaymentId(),
            "PaymentConfirmed",
            request.rawPayload() == null ? "{}" : request.rawPayload()
        ));
        webhook.markCompleted();
        paymentRepository.save(payment);
        paymentWebhookRepository.save(webhook);
    }
}

