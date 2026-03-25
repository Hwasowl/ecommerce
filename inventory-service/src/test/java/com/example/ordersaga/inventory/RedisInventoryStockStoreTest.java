package com.example.ordersaga.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordersaga.inventory.application.InventoryStockStore;
import com.example.ordersaga.inventory.config.InventoryRedisProperties;
import com.example.ordersaga.inventory.domain.Inventory;
import com.example.ordersaga.inventory.infrastructure.stock.RedisInventoryStockStore;
import com.example.ordersaga.inventory.repository.InventoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class RedisInventoryStockStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<String> inventoryReserveScript;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisInventoryStockStore redisInventoryStockStore;

    @BeforeEach
    void setUp() {
        redisInventoryStockStore = new RedisInventoryStockStore(
            stringRedisTemplate,
            inventoryReserveScript,
            inventoryRepository,
            new InventoryRedisProperties("inventory:stock")
        );
    }

    @Test
    @DisplayName("Redis 스크립트가 성공 결과를 반환하면 상품별 before/after 재고를 해석한다.")
    void reserveSuccessfully() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(Inventory.of(101L, 10)));
        when(inventoryRepository.findByProductId(102L)).thenReturn(Optional.of(Inventory.of(102L, 5)));
        when(stringRedisTemplate.execute(eq(inventoryReserveScript), eq(List.of("inventory:stock:101", "inventory:stock:102")), aryEq(new Object[] { "2", "1" })))
            .thenReturn("OK|10,8,5,4");

        InventoryStockStore.ReservationAttempt result = redisInventoryStockStore.reserve(List.of(
            new InventoryStockStore.ReserveCommand(101L, 2),
            new InventoryStockStore.ReserveCommand(102L, 1)
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.reservedStocks()).hasSize(2);
        assertThat(result.reservedStocks().get(0).beforeStock()).isEqualTo(10);
        assertThat(result.reservedStocks().get(0).afterStock()).isEqualTo(8);
        assertThat(result.reservedStocks().get(1).beforeStock()).isEqualTo(5);
        assertThat(result.reservedStocks().get(1).afterStock()).isEqualTo(4);
        verify(valueOperations).setIfAbsent("inventory:stock:101", "10");
        verify(valueOperations).setIfAbsent("inventory:stock:102", "5");
    }

    @Test
    @DisplayName("Redis 스크립트가 실패 결과를 반환하면 실패 코드와 사유를 그대로 전달한다.")
    void returnFailureWhenLuaScriptFails() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(Inventory.of(101L, 1)));
        when(stringRedisTemplate.execute(eq(inventoryReserveScript), eq(List.of("inventory:stock:101")), aryEq(new Object[] { "2" })))
            .thenReturn("FAIL|INSUFFICIENT_STOCK|재고가 부족합니다.");

        InventoryStockStore.ReservationAttempt result = redisInventoryStockStore.reserve(List.of(
            new InventoryStockStore.ReserveCommand(101L, 2)
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.failureCode()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(result.failureReason()).isEqualTo("재고가 부족합니다.");
    }

    @Test
    @DisplayName("Redis 스크립트 실행 결과가 비어 있으면 예외가 발생한다.")
    void failWhenLuaScriptReturnsBlank() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(Inventory.of(101L, 10)));
        when(stringRedisTemplate.execute(eq(inventoryReserveScript), eq(List.of("inventory:stock:101")), aryEq(new Object[] { "1" })))
            .thenReturn(" ");

        assertThatThrownBy(() -> redisInventoryStockStore.reserve(List.of(
            new InventoryStockStore.ReserveCommand(101L, 1)
        )))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Redis 재고 예약 스크립트 실행 결과가 비어 있습니다.");
    }
}
