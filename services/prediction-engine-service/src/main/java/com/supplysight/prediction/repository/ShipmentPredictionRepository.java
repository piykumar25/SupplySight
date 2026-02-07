package com.supplysight.prediction.repository;

import com.supplysight.prediction.entity.ShipmentPrediction;
import com.supplysight.prediction.entity.ShipmentPrediction.DelayRisk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for ShipmentPrediction operations. */
@Repository
public interface ShipmentPredictionRepository extends JpaRepository<ShipmentPrediction, UUID> {

    /** Find latest prediction for a shipment. */
    @Query(
            "SELECT p FROM ShipmentPrediction p WHERE p.tenantId = :tenantId AND p.shipmentId = :shipmentId ORDER BY p.createdAt DESC LIMIT 1")
    Optional<ShipmentPrediction> findLatestByTenantIdAndShipmentId(
            @Param("tenantId") UUID tenantId, @Param("shipmentId") UUID shipmentId);

    /** Find all predictions for a shipment. */
    @Query(
            "SELECT p FROM ShipmentPrediction p WHERE p.tenantId = :tenantId AND p.shipmentId = :shipmentId ORDER BY p.createdAt DESC")
    List<ShipmentPrediction> findByTenantIdAndShipmentId(
            @Param("tenantId") UUID tenantId, @Param("shipmentId") UUID shipmentId);

    /** Find predictions by tenant with pagination. */
    @Query(
            "SELECT p FROM ShipmentPrediction p WHERE p.tenantId = :tenantId ORDER BY p.createdAt DESC")
    Page<ShipmentPrediction> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /** Find predictions by delay risk level. */
    @Query(
            "SELECT p FROM ShipmentPrediction p WHERE p.tenantId = :tenantId AND p.delayRisk = :delayRisk ORDER BY p.createdAt DESC")
    Page<ShipmentPrediction> findByTenantIdAndDelayRisk(
            @Param("tenantId") UUID tenantId,
            @Param("delayRisk") DelayRisk delayRisk,
            Pageable pageable);

    /** Find predictions with anomalies. */
    @Query(
            "SELECT p FROM ShipmentPrediction p WHERE p.tenantId = :tenantId AND p.anomalyDetected = true ORDER BY p.createdAt DESC")
    Page<ShipmentPrediction> findAnomaliesByTenantId(
            @Param("tenantId") UUID tenantId, Pageable pageable);

    /** Check if prediction exists for shipment and event. */
    boolean existsByShipmentIdAndEventId(UUID shipmentId, UUID eventId);
}
