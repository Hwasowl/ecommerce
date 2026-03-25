package com.example.ordersaga.payment.application;

import com.example.ordersaga.payment.domain.OutboxStatus;
import com.example.ordersaga.payment.domain.PaymentOutbox;
import com.example.ordersaga.payment.infrastructure.kafka.PaymentEventPublisher;
import com.example.ordersaga.payment.repository.PaymentOutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxRelay {

    private static final List<OutboxStatus> RETRYABLE_STATUSES = List.of(OutboxStatus.INIT, OutboxStatus.FAILED);

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Scheduled(fixedDelayString = "${payment.outbox.relay-fixed-delay-ms:5000}")
    public void relay() {
        List<PaymentOutbox> outboxes = paymentOutboxRepository.findTop100ByPublishStatusInOrderByCreatedAtAsc(RETRYABLE_STATUSES);
        outboxes.forEach(this::publish);
    }

    private void publish(PaymentOutbox outbox) {
        try {
            paymentEventPublisher.publish(outbox);
            markPublished(outbox.getId());
        } catch (RuntimeException ex) {
            log.warn("결제 아웃박스 이벤트 발행에 실패했습니다. eventId={}", outbox.getEventId(), ex);
            markFailed(outbox.getId());
        }
    }

    @Transactional
    protected void markPublished(Long outboxId) {
        PaymentOutbox outbox = paymentOutboxRepository.findById(outboxId)
            .orElseThrow(() -> new IllegalArgumentException("payment outbox를 찾을 수 없습니다. id=" + outboxId));
        outbox.markPublished();
    }

    @Transactional
    protected void markFailed(Long outboxId) {
        PaymentOutbox outbox = paymentOutboxRepository.findById(outboxId)
            .orElseThrow(() -> new IllegalArgumentException("payment outbox를 찾을 수 없습니다. id=" + outboxId));
        outbox.markFailed();
    }
}
