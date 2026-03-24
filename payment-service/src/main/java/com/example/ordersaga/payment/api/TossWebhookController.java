package com.example.ordersaga.payment.api;

import com.example.ordersaga.payment.application.TossWebhookService;
import com.example.ordersaga.payment.application.dto.TossWebhookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/toss/webhooks")
@Tag(name = "Toss Webhooks", description = "Toss Payments 웹훅 API")
public class TossWebhookController {

    private final TossWebhookService tossWebhookService;

    @PostMapping("/payments")
    @Operation(summary = "Toss 결제 웹훅 수신", description = "Toss Payments가 전달한 결제 상태 변경 웹훅을 수신하고 멱등성 검사를 수행합니다.")
    public ResponseEntity<Map<String, String>> receivePaymentWebhook(
        @RequestHeader(value = "TossPayments-Signature", required = false) String signature,
        @Valid @RequestBody TossWebhookRequest request
    ) {
        tossWebhookService.handle(signature, request);
        return ResponseEntity.ok(Map.of("result", "ACK"));
    }
}

