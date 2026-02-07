package com.supplysight.visibility.repository;

import com.supplysight.visibility.entity.ShipmentTimeline;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for ShipmentTimeline operations. */
@Repository
public interface ShipmentTimelineRepository extends JpaRepository<ShipmentTimeline, UUID> {

    /** Find all timeline events for a shipment, ordered by event time. */
    @Query(
            "SELECT t FROM ShipmentTimeline t WHERE t.tenantId = :tenantId AND t.shipmentId = :shipmentId ORDER BY t.eventTime ASC")
    List<ShipmentTimeline> findByTenantIdAndShipmentIdOrderByEventTime(
            @Param("tenantId") UUID tenantId, @Param("shipmentId") UUID shipmentId);

    /** Check if event already exists in timeline. */
    boolean existsByEventId(UUID eventId);

    /** Find timeline entry by event ID. */
    Optional<ShipmentTimeline> findByEventId(UUID eventId);

    /** Find events within a time range. */
    @Query(
            "SELECT t FROM ShipmentTimeline t WHERE t.tenantId = :tenantId AND t.shipmentId = :shipmentId AND t.eventTime BETWEEN :startTime AND :endTime ORDER BY t.eventTime ASC")
    List<ShipmentTimeline> findByTenantIdAndShipmentIdAndEventTimeBetween(
            @Param("tenantId") UUID tenantId,
            @Param("shipmentId") UUID shipmentId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /** Count events for a shipment. */
    @Query(
            "SELECT COUNT(t) FROM ShipmentTimeline t WHERE t.tenantId = :tenantId AND t.shipmentId = :shipmentId")
    long countByTenantIdAndShipmentId(
            @Param("tenantId") UUID tenantId, @Param("shipmentId") UUID shipmentId);

    /** Find latest event for a shipment. */
    @Query(
            "SELECT t FROM ShipmentTimeline t WHERE t.tenantId = :tenantId AND t.shipmentId = :shipmentId ORDER BY t.eventTime DESC LIMIT 1")
    Optional<ShipmentTimeline> findLatestByTenantIdAndShipmentId(
            @Param("tenantId") UUID tenantId, @Param("shipmentId") UUID shipmentId);
}
