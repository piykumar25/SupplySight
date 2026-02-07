package com.supplysight.audit.repository;

import com.supplysight.audit.entity.AuditStatistics;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for audit statistics queries. */
@Repository
public interface AuditStatisticsRepository extends JpaRepository<AuditStatistics, Long> {

    List<AuditStatistics> findByTenantIdAndStatDateBetweenOrderByStatDateDesc(
            UUID tenantId, LocalDate startDate, LocalDate endDate);

    @Query(
            "SELECT s FROM AuditStatistics s "
                    + "WHERE s.tenantId = :tenantId "
                    + "AND s.statDate BETWEEN :startDate AND :endDate "
                    + "AND s.action = :action "
                    + "ORDER BY s.statDate")
    List<AuditStatistics> findByTenantAndActionAndDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("action") String action,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(
            "SELECT s.action, SUM(s.count) FROM AuditStatistics s "
                    + "WHERE s.tenantId = :tenantId "
                    + "AND s.statDate BETWEEN :startDate AND :endDate "
                    + "GROUP BY s.action "
                    + "ORDER BY SUM(s.count) DESC")
    List<Object[]> getActionSummary(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(
            "SELECT s.statDate, SUM(s.count) FROM AuditStatistics s "
                    + "WHERE s.tenantId = :tenantId "
                    + "AND s.statDate BETWEEN :startDate AND :endDate "
                    + "GROUP BY s.statDate "
                    + "ORDER BY s.statDate")
    List<Object[]> getDailyTotals(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
