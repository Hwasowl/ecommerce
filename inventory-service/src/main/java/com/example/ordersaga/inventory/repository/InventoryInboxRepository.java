package com.example.ordersaga.inventory.repository;

import com.example.ordersaga.inventory.domain.InventoryInbox;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryInboxRepository extends JpaRepository<InventoryInbox, Long> {

    Optional<InventoryInbox> findByEventId(String eventId);
}
