package com.example.ordersaga.payment.repository;

import com.example.ordersaga.payment.domain.PaymentOutbox;
import com.example.ordersaga.payment.domain.OutboxStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    Optional<PaymentOutbox> findByEventId(String eventId);

    List<PaymentOutbox> findTop100ByPublishStatusInOrderByCreatedAtAsc(Collection<OutboxStatus> statuses);
}
