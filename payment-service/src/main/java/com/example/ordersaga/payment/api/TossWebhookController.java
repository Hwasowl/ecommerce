package com.example.ordersaga.payment.api;

import com.example.ordersaga.payment.application.TossWebhookService;
import com.example.ordersaga.payment.application.dto.TossWebhookRequest;
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
public class TossWebhookController {

    private final TossWebhookService tossWebhookService;

    @PostMapping("/payments")
    public ResponseEntity<Map<String, String>> receivePaymentWebhook(
        @RequestHeader(value = "TossPayments-Signature", required = false) String signature,
        @Valid @RequestBody TossWebhookRequest request
    ) {
        tossWebhookService.handle(signature, request);
        return ResponseEntity.ok(Map.of("result", "ACK"));
    }
}
