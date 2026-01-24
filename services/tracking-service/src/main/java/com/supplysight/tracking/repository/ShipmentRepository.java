package com.supplysight.tracking.repository;

import com.supplysight.tracking.domain.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, String> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
}
