package com.supplysight.tracking.repository;

import com.supplysight.tracking.domain.Shipment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByTenantIdAndTrackingNumber(UUID tenantId, String trackingNumber);

    java.util.List<Shipment> findAllByTenantId(UUID tenantId);
}
