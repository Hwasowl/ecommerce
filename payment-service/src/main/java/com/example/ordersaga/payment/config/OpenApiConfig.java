package com.example.ordersaga.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Payment Service API")
                .description("결제 시작과 Toss Payments 웹훅 처리를 담당하는 API 문서")
                .version("v1"));
    }
}

