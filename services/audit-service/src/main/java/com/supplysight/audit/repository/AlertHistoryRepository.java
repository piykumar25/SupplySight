package com.supplysight.audit.repository;

import com.supplysight.audit.entity.AlertHistory;
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

/** Repository for alert history queries. */
@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    Optional<AlertHistory> findByAlertId(UUID alertId);

    Page<AlertHistory> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<AlertHistory> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, String status, Pageable pageable);

    Page<AlertHistory> findByTenantIdAndAlertTypeOrderByCreatedAtDesc(
            UUID tenantId, String alertType, Pageable pageable);

    Page<AlertHistory> findByTenantIdAndShipmentIdOrderByCreatedAtDesc(
            UUID tenantId, UUID shipmentId, Pageable pageable);

    @Query(
            "SELECT a FROM AlertHistory a WHERE a.tenantId = :tenantId "
                    + "AND a.status IN :statuses "
                    + "ORDER BY a.createdAt DESC")
    List<AlertHistory> findOpenAlerts(
            @Param("tenantId") UUID tenantId, @Param("statuses") List<String> statuses);

    @Query(
            "SELECT a.alertType, COUNT(a) FROM AlertHistory a "
                    + "WHERE a.tenantId = :tenantId "
                    + "AND a.createdAt > :since "
                    + "GROUP BY a.alertType")
    List<Object[]> countByAlertType(
            @Param("tenantId") UUID tenantId, @Param("since") Instant since);

    @Query(
            "SELECT a.status, COUNT(a) FROM AlertHistory a "
                    + "WHERE a.tenantId = :tenantId "
                    + "GROUP BY a.status")
    List<Object[]> countByStatus(@Param("tenantId") UUID tenantId);

    @Query(
            "SELECT COUNT(a) FROM AlertHistory a "
                    + "WHERE a.tenantId = :tenantId AND a.status = 'OPEN'")
    long countOpenAlerts(@Param("tenantId") UUID tenantId);
}
