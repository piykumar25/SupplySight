package com.supplysight.prediction.repository;

import com.supplysight.prediction.entity.Alert;
import com.supplysight.prediction.entity.Alert.AlertType;
import com.supplysight.prediction.entity.Alert.Severity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for Alert operations. */
@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    // ==================== BASIC QUERIES ====================

    /** Find all alerts for a tenant with pagination. */
    Page<Alert> findByTenantId(UUID tenantId, Pageable pageable);

    /** Find alerts for a shipment. */
    List<Alert> findByTenantIdAndShipmentIdOrderByCreatedAtDesc(UUID tenantId, UUID shipmentId);

    // ==================== FILTER QUERIES ====================

    /** Find unacknowledged alerts for a tenant. */
    Page<Alert> findByTenantIdAndAcknowledgedFalse(UUID tenantId, Pageable pageable);

    /** Find unresolved alerts for a tenant. */
    Page<Alert> findByTenantIdAndResolvedFalse(UUID tenantId, Pageable pageable);

    /** Find alerts by severity. */
    Page<Alert> findByTenantIdAndSeverity(UUID tenantId, Severity severity, Pageable pageable);

    /** Find alerts by type. */
    Page<Alert> findByTenantIdAndAlertType(UUID tenantId, AlertType alertType, Pageable pageable);

    // ==================== COUNT QUERIES ====================

    /** Count all alerts for a tenant. */
    long countByTenantId(UUID tenantId);

    /** Count unacknowledged alerts. */
    long countByTenantIdAndAcknowledgedFalse(UUID tenantId);

    /** Count unresolved alerts. */
    long countByTenantIdAndResolvedFalse(UUID tenantId);

    // ==================== CUSTOM QUERIES ====================

    /** Find recent alerts (for notification bell). */
    @Query("SELECT a FROM Alert a WHERE a.tenantId = :tenantId ORDER BY a.createdAt DESC")
    List<Alert> findRecentByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /** Find critical unacknowledged alerts (for toast notifications). */
    @Query(
            "SELECT a FROM Alert a WHERE a.tenantId = :tenantId "
                    + "AND a.acknowledged = false "
                    + "AND (a.severity = 'CRITICAL' OR a.alertType = 'DELAY_RISK_HIGH' OR a.alertType = 'ANOMALY_DETECTED') "
                    + "ORDER BY a.createdAt DESC")
    List<Alert> findCriticalUnacknowledgedByTenantId(
            @Param("tenantId") UUID tenantId, Pageable pageable);
}
