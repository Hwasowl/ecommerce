package com.example.ordersaga.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordersaga.order.application.OrderService;
import com.example.ordersaga.order.application.dto.CreateOrderResponse;
import com.example.ordersaga.order.api.OrderController;
import com.example.ordersaga.order.domain.OrderStatus;
import com.example.ordersaga.order.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("주문 생성 요청이 유효하면 CREATED 응답을 반환한다.")
    void createOrderWhenRequestIsValid() throws Exception {
        when(orderService.createOrder(any())).thenReturn(
            new CreateOrderResponse("ORD-1001", OrderStatus.CREATED, BigDecimal.valueOf(130000), "KRW")
        );

        String requestBody = """
            {
              "customerId": 1,
              "currency": "KRW",
              "items": [
                {
                  "productId": 101,
                  "productName": "keyboard",
                  "quantity": 2,
                  "unitPrice": 50000
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value("ORD-1001"))
            .andExpect(jsonPath("$.status").value("CREATED"));

        verify(orderService).createOrder(any());
    }

    @Test
    @DisplayName("주문 상품이 비어 있으면 주문 생성에 실패한다.")
    void failCreateOrderWhenItemsAreEmpty() throws Exception {
        String requestBody = """
            {
              "customerId": 1,
              "currency": "KRW",
              "items": []
            }
            """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORDER-400"))
            .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("주문 통화가 비어 있으면 주문 생성에 실패한다.")
    void failCreateOrderWhenCurrencyIsBlank() throws Exception {
        String requestBody = """
            {
              "customerId": 1,
              "currency": "",
              "items": [
                {
                  "productId": 101,
                  "productName": "keyboard",
                  "quantity": 1,
                  "unitPrice": 50000
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORDER-400"));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("주문 상품 수량이 0 이하이면 주문 생성에 실패한다.")
    void failCreateOrderWhenQuantityIsNotPositive() throws Exception {
        String requestBody = """
            {
              "customerId": 1,
              "currency": "KRW",
              "items": [
                {
                  "productId": 101,
                  "productName": "keyboard",
                  "quantity": 0,
                  "unitPrice": 50000
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORDER-400"));

        verifyNoInteractions(orderService);
    }
}
