package com.example.ordersaga.inventory.infrastructure.stock;

import com.example.ordersaga.inventory.application.InventoryStockStore;
import com.example.ordersaga.inventory.config.InventoryRedisProperties;
import com.example.ordersaga.inventory.domain.Inventory;
import com.example.ordersaga.inventory.repository.InventoryRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisInventoryStockStore implements InventoryStockStore {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<String> inventoryReserveScript;
    private final InventoryRepository inventoryRepository;
    private final InventoryRedisProperties inventoryRedisProperties;

    @Override
    public ReservationAttempt reserve(List<ReserveCommand> commands) {
        initializeStocksIfAbsent(commands);

        List<String> keys = commands.stream()
            .map(command -> stockKey(command.productId()))
            .toList();
        List<String> quantities = commands.stream()
            .map(command -> String.valueOf(command.quantity()))
            .toList();
        Object[] quantityArgs = quantities.toArray();

        String result = stringRedisTemplate.execute(inventoryReserveScript, keys, quantityArgs);
        if (result == null || result.isBlank()) {
            throw new IllegalStateException("Redis 재고 예약 스크립트 실행 결과가 비어 있습니다.");
        }

        return parseResult(commands, result);
    }

    private void initializeStocksIfAbsent(List<ReserveCommand> commands) {
        for (ReserveCommand command : commands) {
            Inventory inventory = inventoryRepository.findByProductId(command.productId())
                .orElse(null);
            if (inventory == null) {
                continue;
            }
            stringRedisTemplate.opsForValue().setIfAbsent(
                stockKey(command.productId()),
                String.valueOf(inventory.getAvailableStock())
            );
        }
    }

    private ReservationAttempt parseResult(List<ReserveCommand> commands, String result) {
        if (result.startsWith("FAIL|")) {
            String[] parts = result.split("\\|", 3);
            return ReservationAttempt.failure(parts[1], parts.length > 2 ? parts[2] : "재고 예약에 실패했습니다.");
        }

        String[] payload = result.split("\\|", 2);
        if (payload.length < 2 || payload[1].isBlank()) {
            throw new IllegalStateException("Redis 재고 예약 스크립트 결과 형식이 올바르지 않습니다.");
        }

        String[] values = payload[1].split(",");
        if (values.length != commands.size() * 2) {
            throw new IllegalStateException("Redis 재고 예약 스크립트 결과 개수가 예상과 다릅니다.");
        }

        List<ReservedStock> reservedStocks = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            ReserveCommand command = commands.get(i);
            int beforeStock = Integer.parseInt(values[i * 2]);
            int afterStock = Integer.parseInt(values[i * 2 + 1]);
            reservedStocks.add(new ReservedStock(command.productId(), command.quantity(), beforeStock, afterStock));
        }
        return ReservationAttempt.success(reservedStocks);
    }

    private String stockKey(Long productId) {
        return inventoryRedisProperties.stockKeyPrefix() + ":" + productId;
    }
}
