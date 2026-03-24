package com.example.ordersaga.inventory.repository;

import com.example.ordersaga.inventory.domain.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
}
