package com.example.ordersaga.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Order Service API")
                .description("주문 생성과 상태 관리를 담당하는 API 문서")
                .version("v1"));
    }
}

