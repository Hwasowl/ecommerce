package com.example.ordersaga.payment.infrastructure.toss;

import com.example.ordersaga.payment.domain.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoOpTossPaymentCancelClient implements TossPaymentCancelClient {

    @Override
    public void cancel(Payment payment, String reason) {
        log.info("토스 결제 취소를 요청했습니다. paymentId={}, orderId={}", payment.getPaymentId(), payment.getOrderId());
    }
}
