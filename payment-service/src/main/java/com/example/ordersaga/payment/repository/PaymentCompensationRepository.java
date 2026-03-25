package com.example.ordersaga.payment.repository;

import com.example.ordersaga.payment.domain.PaymentCompensation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCompensationRepository extends JpaRepository<PaymentCompensation, Long> {

    Optional<PaymentCompensation> findBySourceEventId(String sourceEventId);
}
