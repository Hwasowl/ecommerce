package com.example.ordersaga.payment.application;

import com.example.ordersaga.payment.application.dto.PaymentConfirmedEvent;
import com.example.ordersaga.payment.application.dto.TossWebhookRequest;
import com.example.ordersaga.payment.domain.Payment;
import com.example.ordersaga.payment.domain.PaymentOutbox;
import com.example.ordersaga.payment.domain.PaymentWebhook;
import com.example.ordersaga.payment.exception.BusinessException;
import com.example.ordersaga.payment.exception.ErrorCode;
import com.example.ordersaga.payment.repository.PaymentOutboxRepository;
import com.example.ordersaga.payment.repository.PaymentRepository;
import com.example.ordersaga.payment.repository.PaymentWebhookRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TossWebhookService {

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookRepository paymentWebhookRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final ObjectMapper objectMapper;

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
            toPaymentConfirmedPayload(request, payment)
        ));
        webhook.markCompleted();
        paymentRepository.save(payment);
        paymentWebhookRepository.save(webhook);
    }

    private String toPaymentConfirmedPayload(TossWebhookRequest request, Payment payment) {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
            request.eventId(),
            "PaymentConfirmed",
            LocalDateTime.now(),
            payment.getOrderId(),
            payment.getPaymentId(),
            request.paymentKey(),
            payment.getCustomerId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getOrderName(),
            payment.getItemSnapshots().stream()
                .map(item -> new PaymentConfirmedEvent.PaymentConfirmedItem(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getLineAmount()
                ))
                .toList()
        );
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("결제 확정 이벤트 페이로드 직렬화에 실패했습니다.", ex);
        }
    }
}

