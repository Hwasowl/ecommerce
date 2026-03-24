package com.example.ordersaga.payment.repository;

import com.example.ordersaga.payment.domain.PaymentOutbox;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    Optional<PaymentOutbox> findByEventId(String eventId);
}
