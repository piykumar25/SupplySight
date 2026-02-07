package com.supplysight.audit.repository;

import com.supplysight.audit.entity.AuditLog;
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

/** Repository for audit log queries. */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findByEventId(UUID eventId);

    Page<AuditLog> findByTenantIdOrderByEventTimeDesc(UUID tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndUserIdOrderByEventTimeDesc(
            UUID tenantId, UUID userId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndActionOrderByEventTimeDesc(
            UUID tenantId, String action, Pageable pageable);

    Page<AuditLog> findByTenantIdAndResourceTypeAndResourceIdOrderByEventTimeDesc(
            UUID tenantId, String resourceType, String resourceId, Pageable pageable);

    @Query(
            "SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId "
                    + "AND a.eventTime BETWEEN :startTime AND :endTime "
                    + "ORDER BY a.eventTime DESC")
    Page<AuditLog> findByTenantIdAndTimeRange(
            @Param("tenantId") UUID tenantId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    @Query(
            "SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId "
                    + "AND (:action IS NULL OR a.action = :action) "
                    + "AND (:userId IS NULL OR a.userId = :userId) "
                    + "AND (:resourceType IS NULL OR a.resourceType = :resourceType) "
                    + "AND a.eventTime BETWEEN :startTime AND :endTime "
                    + "ORDER BY a.eventTime DESC")
    Page<AuditLog> searchAuditLogs(
            @Param("tenantId") UUID tenantId,
            @Param("action") String action,
            @Param("userId") UUID userId,
            @Param("resourceType") String resourceType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    @Query(
            "SELECT a.action, COUNT(a) FROM AuditLog a "
                    + "WHERE a.tenantId = :tenantId "
                    + "AND a.eventTime BETWEEN :startTime AND :endTime "
                    + "GROUP BY a.action")
    List<Object[]> countByActionForTenant(
            @Param("tenantId") UUID tenantId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    Optional<AuditLog> findByCorrelationId(String correlationId);

    @Query(
            "SELECT COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId "
                    + "AND a.eventTime > :since")
    long countRecentByTenant(@Param("tenantId") UUID tenantId, @Param("since") Instant since);
}
