package com.supplysight.prediction.repository;

import com.supplysight.prediction.entity.Alert;
import com.supplysight.prediction.entity.Alert.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Alert operations.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    /**
     * Find alerts for a shipment.
     */
    @Query("SELECT a FROM Alert a WHERE a.tenantId = :tenantId AND a.shipmentId = :shipmentId ORDER BY a.createdAt DESC")
    List<Alert> findByTenantIdAndShipmentId(
            @Param("tenantId") UUID tenantId,
            @Param("shipmentId") UUID shipmentId
    );

    /**
     * Find unacknowledged alerts for a tenant.
     */
    @Query("SELECT a FROM Alert a WHERE a.tenantId = :tenantId AND a.acknowledged = false ORDER BY a.createdAt DESC")
    Page<Alert> findUnacknowledgedByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Find alerts by type.
     */
    @Query("SELECT a FROM Alert a WHERE a.tenantId = :tenantId AND a.alertType = :alertType ORDER BY a.createdAt DESC")
    Page<Alert> findByTenantIdAndAlertType(
            @Param("tenantId") UUID tenantId,
            @Param("alertType") AlertType alertType,
            Pageable pageable
    );

    /**
     * Find all alerts for tenant with pagination.
     */
    @Query("SELECT a FROM Alert a WHERE a.tenantId = :tenantId ORDER BY a.createdAt DESC")
    Page<Alert> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Count unacknowledged alerts.
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.tenantId = :tenantId AND a.acknowledged = false")
    long countUnacknowledgedByTenantId(@Param("tenantId") UUID tenantId);
}
