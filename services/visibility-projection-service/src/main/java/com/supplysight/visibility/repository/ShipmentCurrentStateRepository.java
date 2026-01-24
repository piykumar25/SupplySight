package com.supplysight.visibility.repository;

import com.supplysight.visibility.entity.ShipmentCurrentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ShipmentCurrentState operations.
 */
@Repository
public interface ShipmentCurrentStateRepository extends JpaRepository<ShipmentCurrentState, UUID> {

    /**
     * Find current state by tenant and shipment.
     */
    @Query("SELECT s FROM ShipmentCurrentState s WHERE s.tenantId = :tenantId AND s.shipmentId = :shipmentId")
    Optional<ShipmentCurrentState> findByTenantIdAndShipmentId(
            @Param("tenantId") UUID tenantId,
            @Param("shipmentId") UUID shipmentId
    );

    /**
     * Find all shipments for a tenant with pagination.
     */
    @Query("SELECT s FROM ShipmentCurrentState s WHERE s.tenantId = :tenantId ORDER BY s.updatedAt DESC")
    Page<ShipmentCurrentState> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Find shipments by status.
     */
    @Query("SELECT s FROM ShipmentCurrentState s WHERE s.tenantId = :tenantId AND s.status = :status ORDER BY s.updatedAt DESC")
    Page<ShipmentCurrentState> findByTenantIdAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("status") String status,
            Pageable pageable
    );

    /**
     * Find in-transit shipments for a tenant.
     */
    @Query("SELECT s FROM ShipmentCurrentState s WHERE s.tenantId = :tenantId AND s.status IN ('IN_TRANSIT', 'OUT_FOR_DELIVERY')")
    List<ShipmentCurrentState> findActiveShipments(@Param("tenantId") UUID tenantId);

    /**
     * Count shipments by status.
     */
    @Query("SELECT s.status, COUNT(s) FROM ShipmentCurrentState s WHERE s.tenantId = :tenantId GROUP BY s.status")
    List<Object[]> countByTenantIdGroupByStatus(@Param("tenantId") UUID tenantId);

    /**
     * Check if shipment exists.
     */
    @Query("SELECT COUNT(s) > 0 FROM ShipmentCurrentState s WHERE s.tenantId = :tenantId AND s.shipmentId = :shipmentId")
    boolean existsByTenantIdAndShipmentId(@Param("tenantId") UUID tenantId, @Param("shipmentId") UUID shipmentId);
}
