package com.example.ordersaga.payment.application;

import com.example.ordersaga.payment.application.dto.TossWebhookRequest;
import com.example.ordersaga.payment.domain.PaymentWebhook;
import com.example.ordersaga.payment.repository.PaymentWebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TossWebhookService {

    private final PaymentWebhookRepository paymentWebhookRepository;

    @Transactional
    public void handle(String signature, TossWebhookRequest request) {
        if (paymentWebhookRepository.findByEventId(request.eventId()).isPresent()) {
            return;
        }

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
        webhook.markCompleted();
        paymentWebhookRepository.save(webhook);
    }
}
