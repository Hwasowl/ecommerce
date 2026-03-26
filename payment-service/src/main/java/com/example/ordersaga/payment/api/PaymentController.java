package com.example.ordersaga.payment.api;

import com.example.ordersaga.payment.application.PaymentService;
import com.example.ordersaga.payment.application.dto.CreatePaymentRequest;
import com.example.ordersaga.payment.application.dto.CreatePaymentResponse;
import com.example.ordersaga.payment.application.dto.PaymentStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "결제 API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "결제 시작", description = "주문 기준으로 결제 엔티티를 생성하고 Toss Payments 연동에 필요한 값을 반환합니다.")
    public CreatePaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "결제 상태 조회", description = "paymentId 기준으로 현재 결제 상태와 기본 정보를 반환합니다.")
    public PaymentStatusResponse getPaymentStatus(@PathVariable String paymentId) {
        return paymentService.getPaymentStatus(paymentId);
    }
}

