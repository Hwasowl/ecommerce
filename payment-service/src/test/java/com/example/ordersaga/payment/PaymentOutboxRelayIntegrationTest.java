package com.example.ordersaga.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.ordersaga.payment.application.PaymentOutboxRelay;
import com.example.ordersaga.payment.domain.OutboxStatus;
import com.example.ordersaga.payment.domain.PaymentOutbox;
import com.example.ordersaga.payment.infrastructure.kafka.PaymentEventPublisher;
import com.example.ordersaga.payment.repository.PaymentOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Transactional
class PaymentOutboxRelayIntegrationTest {

    @Autowired
    private PaymentOutboxRelay paymentOutboxRelay;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @MockBean
    private PaymentEventPublisher paymentEventPublisher;

    @Test
    @DisplayName("relay가 미발행 outbox를 발행하면 PUBLISHED로 변경한다.")
    void publishInitOutboxAndMarkPublished() {
        PaymentOutbox outbox = paymentOutboxRepository.save(
            PaymentOutbox.init("event-relay-1", "PAYMENT", "PAY-1001", "PaymentConfirmed", "{\"eventId\":\"event-relay-1\"}")
        );

        paymentOutboxRelay.relay();

        PaymentOutbox publishedOutbox = paymentOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(publishedOutbox.getPublishStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publishedOutbox.getPublishedAt()).isNotNull();
        verify(paymentEventPublisher, times(1)).publish(publishedOutbox);
    }

    @Test
    @DisplayName("relay 발행이 실패하면 outbox를 FAILED로 변경한다.")
    void markFailedWhenPublishFails() {
        PaymentOutbox outbox = paymentOutboxRepository.save(
            PaymentOutbox.init("event-relay-2", "PAYMENT", "PAY-1002", "PaymentConfirmed", "{\"eventId\":\"event-relay-2\"}")
        );
        doThrow(new RuntimeException("kafka unavailable")).when(paymentEventPublisher).publish(outbox);

        paymentOutboxRelay.relay();

        PaymentOutbox failedOutbox = paymentOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(failedOutbox.getPublishStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(failedOutbox.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("이미 PUBLISHED인 outbox는 relay 대상에서 제외한다.")
    void skipPublishedOutbox() {
        PaymentOutbox outbox = paymentOutboxRepository.save(
            PaymentOutbox.init("event-relay-3", "PAYMENT", "PAY-1003", "PaymentConfirmed", "{\"eventId\":\"event-relay-3\"}")
        );
        outbox.markPublished();

        paymentOutboxRelay.relay();

        verify(paymentEventPublisher, times(0)).publish(outbox);
    }
}
