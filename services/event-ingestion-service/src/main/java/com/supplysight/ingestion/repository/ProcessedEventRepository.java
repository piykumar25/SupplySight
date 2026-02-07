package com.supplysight.ingestion.repository;

import com.supplysight.ingestion.entity.ProcessedEvent;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for ProcessedEvent operations (deduplication tracking). */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    /** Check if event has already been processed. */
    boolean existsByEventId(UUID eventId);

    /** Find all processed event IDs from a collection. */
    @Query("SELECT p.eventId FROM ProcessedEvent p WHERE p.eventId IN :eventIds")
    List<UUID> findExistingEventIds(@Param("eventIds") Collection<UUID> eventIds);

    /** Delete old processed event records for cleanup. */
    @Modifying
    @Query("DELETE FROM ProcessedEvent p WHERE p.processedAt < :cutoffTime")
    int deleteOlderThan(@Param("cutoffTime") Instant cutoffTime);

    /** Count processed events by tenant. */
    @Query("SELECT COUNT(p) FROM ProcessedEvent p WHERE p.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
