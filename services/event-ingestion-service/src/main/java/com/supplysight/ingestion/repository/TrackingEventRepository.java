package com.supplysight.ingestion.repository;

import com.supplysight.ingestion.entity.TrackingEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for TrackingEventEntity operations. */
@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEventEntity, UUID> {

    /** Check if event with given eventId already exists. */
    boolean existsByEventId(UUID eventId);

    /** Find event by eventId. */
    Optional<TrackingEventEntity> findByEventId(UUID eventId);

    /** Find all events for a shipment ordered by event time. */
    @Query(
            "SELECT e FROM TrackingEventEntity e WHERE e.tenantId = :tenantId AND e.shipmentId = :shipmentId ORDER BY e.eventTime ASC")
    List<TrackingEventEntity> findByTenantIdAndShipmentIdOrderByEventTime(
            @Param("tenantId") UUID tenantId, @Param("shipmentId") UUID shipmentId);

    /** Find events by tenant with pagination. */
    @Query(
            "SELECT e FROM TrackingEventEntity e WHERE e.tenantId = :tenantId ORDER BY e.ingestedAt DESC")
    Page<TrackingEventEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /** Find events by tenant and time range. */
    @Query(
            "SELECT e FROM TrackingEventEntity e WHERE e.tenantId = :tenantId AND e.eventTime BETWEEN :startTime AND :endTime ORDER BY e.eventTime ASC")
    List<TrackingEventEntity> findByTenantIdAndEventTimeBetween(
            @Param("tenantId") UUID tenantId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /** Count events by tenant and shipment. */
    @Query(
            "SELECT COUNT(e) FROM TrackingEventEntity e WHERE e.tenantId = :tenantId AND e.shipmentId = :shipmentId")
    long countByTenantIdAndShipmentId(
            @Param("tenantId") UUID tenantId, @Param("shipmentId") UUID shipmentId);

    /** Find latest event for a shipment. */
    @Query(
            "SELECT e FROM TrackingEventEntity e WHERE e.tenantId = :tenantId AND e.shipmentId = :shipmentId ORDER BY e.eventTime DESC LIMIT 1")
    Optional<TrackingEventEntity> findLatestByTenantIdAndShipmentId(
            @Param("tenantId") UUID tenantId, @Param("shipmentId") UUID shipmentId);
}
