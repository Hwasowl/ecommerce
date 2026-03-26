package com.example.ordersaga.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordersaga.payment.api.PaymentController;
import com.example.ordersaga.payment.application.PaymentService;
import com.example.ordersaga.payment.application.dto.CreatePaymentResponse;
import com.example.ordersaga.payment.application.dto.PaymentStatusResponse;
import com.example.ordersaga.payment.domain.PaymentStatus;
import com.example.ordersaga.payment.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("결제 시작 요청이 유효하면 PENDING 응답을 반환한다.")
    void createPaymentWhenRequestIsValid() throws Exception {
        when(paymentService.createPayment(any())).thenReturn(
            new CreatePaymentResponse(
                "PAY-1001",
                "ORD-1001",
                PaymentStatus.PENDING,
                BigDecimal.valueOf(130000),
                "KRW",
                "test_client_key",
                "http://localhost:3000/payments/success",
                "http://localhost:3000/payments/fail"
            )
        );

        String requestBody = """
            {
              "orderId": "ORD-1001",
              "customerId": 1,
              "amount": 130000,
              "currency": "KRW",
              "orderName": "무선 키보드",
              "items": [
                {
                  "productId": 101,
                  "productName": "keyboard",
                  "quantity": 2,
                  "unitPrice": 65000
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value("PAY-1001"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService).createPayment(any());
    }

    @Test
    @DisplayName("주문 ID가 비어 있으면 결제 시작에 실패한다.")
    void failCreatePaymentWhenOrderIdIsBlank() throws Exception {
        String requestBody = """
            {
              "orderId": "",
              "customerId": 1,
              "amount": 130000,
              "currency": "KRW",
              "orderName": "무선 키보드",
              "items": [
                {
                  "productId": 101,
                  "productName": "keyboard",
                  "quantity": 2,
                  "unitPrice": 65000
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PAYMENT-400"));

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("결제 금액이 음수이면 결제 시작에 실패한다.")
    void failCreatePaymentWhenAmountIsNegative() throws Exception {
        String requestBody = """
            {
              "orderId": "ORD-1001",
              "customerId": 1,
              "amount": -1,
              "currency": "KRW",
              "orderName": "무선 키보드",
              "items": [
                {
                  "productId": 101,
                  "productName": "keyboard",
                  "quantity": 2,
                  "unitPrice": 65000
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PAYMENT-400"));

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("결제 상태 조회 요청이 유효하면 현재 상태를 반환한다.")
    void getPaymentStatusWhenPaymentExists() throws Exception {
        when(paymentService.getPaymentStatus("PAY-1001")).thenReturn(
            new PaymentStatusResponse(
                "PAY-1001",
                "ORD-1001",
                1L,
                PaymentStatus.CONFIRMED,
                BigDecimal.valueOf(130000),
                "KRW",
                "무선 키보드",
                "PK-1001",
                "TOSS_PAYMENTS",
                LocalDateTime.of(2026, 3, 26, 11, 0),
                null,
                LocalDateTime.of(2026, 3, 26, 10, 59),
                LocalDateTime.of(2026, 3, 26, 11, 0)
            )
        );

        mockMvc.perform(get("/api/v1/payments/PAY-1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value("PAY-1001"))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(paymentService).getPaymentStatus("PAY-1001");
    }
}
