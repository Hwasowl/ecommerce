package com.example.ordersaga.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordersaga.payment.api.TossWebhookController;
import com.example.ordersaga.payment.application.TossWebhookService;
import com.example.ordersaga.payment.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TossWebhookController.class)
@Import(GlobalExceptionHandler.class)
class TossWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TossWebhookService tossWebhookService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("웹훅 요청이 유효하면 ACK를 반환한다.")
    void acknowledgeWebhookWhenRequestIsValid() throws Exception {
        String requestBody = """
            {
              "eventId": "event-1",
              "eventType": "PAYMENT_STATUS_CHANGED",
              "paymentId": "PAY-1001",
              "orderId": "ORD-1001",
              "paymentKey": "payment-key-1",
              "status": "DONE",
              "rawPayload": "payload"
            }
            """;

        mockMvc.perform(post("/api/v1/toss/webhooks/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .header("TossPayments-Signature", "signature-1")
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("ACK"));

        verify(tossWebhookService).handle(eq("signature-1"), any());
    }

    @Test
    @DisplayName("eventId가 비어 있으면 웹훅 처리에 실패한다.")
    void failWebhookWhenEventIdIsBlank() throws Exception {
        String requestBody = """
            {
              "eventId": "",
              "eventType": "PAYMENT_STATUS_CHANGED",
              "paymentId": "PAY-1001",
              "orderId": "ORD-1001",
              "paymentKey": "payment-key-1",
              "status": "DONE",
              "rawPayload": "payload"
            }
            """;

        mockMvc.perform(post("/api/v1/toss/webhooks/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PAYMENT-400"));

        verifyNoInteractions(tossWebhookService);
    }

    @Test
    @DisplayName("paymentId가 비어 있으면 웹훅 처리에 실패한다.")
    void failWebhookWhenPaymentIdIsBlank() throws Exception {
        String requestBody = """
            {
              "eventId": "event-1",
              "eventType": "PAYMENT_STATUS_CHANGED",
              "paymentId": "",
              "orderId": "ORD-1001",
              "paymentKey": "payment-key-1",
              "status": "DONE",
              "rawPayload": "payload"
            }
            """;

        mockMvc.perform(post("/api/v1/toss/webhooks/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PAYMENT-400"));

        verifyNoInteractions(tossWebhookService);
    }
}
