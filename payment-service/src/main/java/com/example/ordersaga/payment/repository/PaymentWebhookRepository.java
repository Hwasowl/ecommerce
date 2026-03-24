package com.example.ordersaga.payment.repository;

import com.example.ordersaga.payment.domain.PaymentWebhook;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, Long> {

    Optional<PaymentWebhook> findByEventId(String eventId);
}
