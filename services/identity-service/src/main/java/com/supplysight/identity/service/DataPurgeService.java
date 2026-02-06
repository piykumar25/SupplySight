package com.supplysight.identity.service;

import com.supplysight.identity.entity.RetentionPolicy;
import com.supplysight.identity.repository.RetentionPolicyRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled service to purge expired data based on tenant retention policies.
 */
@Service
public class DataPurgeService {

    private static final Logger log = LoggerFactory.getLogger(DataPurgeService.class);

    private final RetentionPolicyRepository policyRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Counter purgedEventsCounter;
    private final Counter purgedAlertsCounter;
    private final Counter purgedShipmentsCounter;

    public DataPurgeService(
            RetentionPolicyRepository policyRepository,
            JdbcTemplate jdbcTemplate,
            MeterRegistry meterRegistry) {
        this.policyRepository = policyRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.purgedEventsCounter = Counter.builder("data_purge_events_total")
                .description("Total events purged")
                .register(meterRegistry);
        this.purgedAlertsCounter = Counter.builder("data_purge_alerts_total")
                .description("Total alerts purged")
                .register(meterRegistry);
        this.purgedShipmentsCounter = Counter.builder("data_purge_shipments_total")
                .description("Total shipments purged")
                .register(meterRegistry);
    }

    /**
     * Run data purge job daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void executeScheduledPurge() {
        log.info("Starting scheduled data purge job");

        List<RetentionPolicy> policies = policyRepository.findAll();
        int totalPurged = 0;

        for (RetentionPolicy policy : policies) {
            try {
                int purged = purgeDataForTenant(policy);
                totalPurged += purged;
            } catch (Exception e) {
                log.error("Failed to purge data for tenant {}: {}", policy.getTenantId(), e.getMessage());
            }
        }

        log.info("Completed scheduled data purge. Total records purged: {}", totalPurged);
    }

    /**
     * Purge data for a specific tenant based on their retention policy.
     */
    @Transactional
    public int purgeDataForTenant(RetentionPolicy policy) {
        UUID tenantId = policy.getTenantId();
        int totalPurged = 0;

        // Purge old events
        Instant eventCutoff = Instant.now().minus(policy.getEventRetentionDays(), ChronoUnit.DAYS);
        int eventsPurged = purgeEvents(tenantId, eventCutoff, policy.isSoftDeleteEnabled());
        purgedEventsCounter.increment(eventsPurged);
        totalPurged += eventsPurged;

        // Purge old alerts
        Instant alertCutoff = Instant.now().minus(policy.getAlertRetentionDays(), ChronoUnit.DAYS);
        int alertsPurged = purgeAlerts(tenantId, alertCutoff, policy.isSoftDeleteEnabled());
        purgedAlertsCounter.increment(alertsPurged);
        totalPurged += alertsPurged;

        // Purge completed shipments
        Instant shipmentCutoff = Instant.now().minus(policy.getShipmentRetentionDays(), ChronoUnit.DAYS);
        int shipmentsPurged = purgeShipments(tenantId, shipmentCutoff, policy.isSoftDeleteEnabled());
        purgedShipmentsCounter.increment(shipmentsPurged);
        totalPurged += shipmentsPurged;

        // Purge soft-deleted records past grace period
        if (policy.isSoftDeleteEnabled()) {
            Instant graceCutoff = Instant.now().minus(policy.getSoftDeleteGraceDays(), ChronoUnit.DAYS);
            int permanentlyDeleted = permanentlyDeleteSoftDeleted(tenantId, graceCutoff);
            totalPurged += permanentlyDeleted;
        }

        log.info("Purged {} records for tenant {}", totalPurged, tenantId);
        return totalPurged;
    }

    private int purgeEvents(UUID tenantId, Instant cutoff, boolean softDelete) {
        if (softDelete) {
            return jdbcTemplate.update(
                    "UPDATE visibility.shipment_events SET deleted_at = NOW() " +
                            "WHERE tenant_id = ? AND event_timestamp < ? AND deleted_at IS NULL",
                    tenantId, cutoff);
        } else {
            return jdbcTemplate.update(
                    "DELETE FROM visibility.shipment_events " +
                            "WHERE tenant_id = ? AND event_timestamp < ?",
                    tenantId, cutoff);
        }
    }

    private int purgeAlerts(UUID tenantId, Instant cutoff, boolean softDelete) {
        if (softDelete) {
            return jdbcTemplate.update(
                    "UPDATE prediction.alerts SET deleted_at = NOW() " +
                            "WHERE tenant_id = ? AND created_at < ? AND deleted_at IS NULL",
                    tenantId, cutoff);
        } else {
            return jdbcTemplate.update(
                    "DELETE FROM prediction.alerts " +
                            "WHERE tenant_id = ? AND created_at < ?",
                    tenantId, cutoff);
        }
    }

    private int purgeShipments(UUID tenantId, Instant cutoff, boolean softDelete) {
        if (softDelete) {
            return jdbcTemplate.update(
                    "UPDATE visibility.shipments SET deleted_at = NOW() " +
                            "WHERE tenant_id = ? AND status = 'DELIVERED' AND updated_at < ? AND deleted_at IS NULL",
                    tenantId, cutoff);
        } else {
            return jdbcTemplate.update(
                    "DELETE FROM visibility.shipments " +
                            "WHERE tenant_id = ? AND status = 'DELIVERED' AND updated_at < ?",
                    tenantId, cutoff);
        }
    }

    private int permanentlyDeleteSoftDeleted(UUID tenantId, Instant graceCutoff) {
        int total = 0;

        total += jdbcTemplate.update(
                "DELETE FROM visibility.shipment_events WHERE tenant_id = ? AND deleted_at < ?",
                tenantId, graceCutoff);

        total += jdbcTemplate.update(
                "DELETE FROM prediction.alerts WHERE tenant_id = ? AND deleted_at < ?",
                tenantId, graceCutoff);

        total += jdbcTemplate.update(
                "DELETE FROM visibility.shipments WHERE tenant_id = ? AND deleted_at < ?",
                tenantId, graceCutoff);

        return total;
    }

    /**
     * GDPR: Complete tenant data deletion.
     * This permanently deletes ALL data for a tenant - use with extreme caution.
     */
    @Transactional
    public void deleteTenantData(UUID tenantId) {
        log.warn("GDPR: Initiating complete data deletion for tenant {}", tenantId);

        // Delete in order respecting foreign keys
        jdbcTemplate.update("DELETE FROM visibility.shipment_events WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM prediction.alerts WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM prediction.predictions WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM visibility.shipments WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM identity.retention_policies WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM identity.tenant_quotas WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM identity.api_keys WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM identity.users WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM identity.tenants WHERE id = ?", tenantId);

        log.warn("GDPR: Completed data deletion for tenant {}", tenantId);
    }
}
