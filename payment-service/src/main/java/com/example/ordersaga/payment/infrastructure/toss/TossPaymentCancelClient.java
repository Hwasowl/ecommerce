package com.example.ordersaga.payment.infrastructure.toss;

import com.example.ordersaga.payment.domain.Payment;

public interface TossPaymentCancelClient {

    void cancel(Payment payment, String reason);
}
