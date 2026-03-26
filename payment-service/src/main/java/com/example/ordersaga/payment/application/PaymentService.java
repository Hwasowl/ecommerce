package com.example.ordersaga.payment.application;

import com.example.ordersaga.payment.application.dto.CreatePaymentItemRequest;
import com.example.ordersaga.payment.application.dto.CreatePaymentRequest;
import com.example.ordersaga.payment.application.dto.CreatePaymentResponse;
import com.example.ordersaga.payment.application.dto.PaymentStatusResponse;
import com.example.ordersaga.payment.domain.Payment;
import com.example.ordersaga.payment.exception.BusinessException;
import com.example.ordersaga.payment.exception.ErrorCode;
import com.example.ordersaga.payment.domain.PaymentItemSnapshot;
import com.example.ordersaga.payment.infrastructure.toss.TossPaymentsProperties;
import com.example.ordersaga.payment.repository.PaymentRepository;
import java.math.BigDecimal;
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
            request.customerId(),
            request.amount(),
            request.currency(),
            request.orderName()
        );
        request.items().forEach(item -> payment.addItemSnapshot(toItemSnapshot(item)));
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

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.PAYMENT_NOT_FOUND,
                "결제 정보를 찾을 수 없습니다. paymentId=" + paymentId
            ));

        return new PaymentStatusResponse(
            payment.getPaymentId(),
            payment.getOrderId(),
            payment.getCustomerId(),
            payment.getStatus(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getOrderName(),
            payment.getPaymentKey(),
            payment.getPgProvider(),
            payment.getApprovedAt(),
            payment.getCanceledAt(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }

    private PaymentItemSnapshot toItemSnapshot(CreatePaymentItemRequest item) {
        return PaymentItemSnapshot.of(
            item.productId(),
            item.productName(),
            item.quantity(),
            item.unitPrice(),
            item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))
        );
    }
}

