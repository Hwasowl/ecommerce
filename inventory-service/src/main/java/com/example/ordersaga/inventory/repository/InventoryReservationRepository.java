package com.example.ordersaga.inventory.repository;

import com.example.ordersaga.inventory.domain.InventoryReservation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByReservationId(String reservationId);
}
