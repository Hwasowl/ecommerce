package com.example.ordersaga.inventory;

import com.example.ordersaga.inventory.config.InventoryRedisProperties;
import com.example.ordersaga.inventory.infrastructure.kafka.InventoryKafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@EnableConfigurationProperties({InventoryRedisProperties.class, InventoryKafkaTopicsProperties.class})
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
