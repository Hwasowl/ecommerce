package com.example.ordersaga.payment.repository;

import com.example.ordersaga.payment.domain.PaymentCompensation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCompensationRepository extends JpaRepository<PaymentCompensation, Long> {
}
