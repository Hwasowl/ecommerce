package com.example.ordersaga.payment.application;

import com.example.ordersaga.payment.application.dto.CreatePaymentRequest;
import com.example.ordersaga.payment.application.dto.CreatePaymentResponse;
import com.example.ordersaga.payment.domain.Payment;
import com.example.ordersaga.payment.infrastructure.toss.TossPaymentsProperties;
import com.example.ordersaga.payment.repository.PaymentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentsProperties tossPaymentsProperties;

    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        String paymentId = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        Payment payment = Payment.pending(
            paymentId,
            request.orderId(),
            request.amount(),
            request.currency()
        );
        paymentRepository.save(payment);

        return new CreatePaymentResponse(
            payment.getPaymentId(),
            payment.getOrderId(),
            payment.getStatus(),
            payment.getAmount(),
            payment.getCurrency(),
            tossPaymentsProperties.clientKey(),
            tossPaymentsProperties.successUrl(),
            tossPaymentsProperties.failUrl()
        );
    }
}

