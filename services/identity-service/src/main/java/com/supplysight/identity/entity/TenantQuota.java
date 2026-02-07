package com.supplysight.identity.entity;

import com.supplysight.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity representing tenant-level quotas and limits. Defines the maximum resources a tenant can
 * consume.
 */
@Entity
@Table(name = "tenant_quotas", schema = "identity")
public class TenantQuota extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "max_active_shipments", nullable = false)
    private Integer maxActiveShipments = 1000;

    @Column(name = "max_events_per_second", nullable = false)
    private Integer maxEventsPerSecond = 100;

    @Column(name = "max_sse_connections", nullable = false)
    private Integer maxSseConnections = 50;

    @Column(name = "event_retention_days", nullable = false)
    private Integer eventRetentionDays = 90;

    @Column(name = "alert_retention_days", nullable = false)
    private Integer alertRetentionDays = 30;

    @Column(name = "prediction_retention_days", nullable = false)
    private Integer predictionRetentionDays = 30;

    public TenantQuota() {
        super();
    }

    public TenantQuota(UUID tenantId) {
        super();
        this.tenantId = tenantId;
    }

    // Getters and Setters
    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getMaxActiveShipments() {
        return maxActiveShipments;
    }

    public void setMaxActiveShipments(Integer maxActiveShipments) {
        this.maxActiveShipments = maxActiveShipments;
    }

    public Integer getMaxEventsPerSecond() {
        return maxEventsPerSecond;
    }

    public void setMaxEventsPerSecond(Integer maxEventsPerSecond) {
        this.maxEventsPerSecond = maxEventsPerSecond;
    }

    public Integer getMaxSseConnections() {
        return maxSseConnections;
    }

    public void setMaxSseConnections(Integer maxSseConnections) {
        this.maxSseConnections = maxSseConnections;
    }

    public Integer getEventRetentionDays() {
        return eventRetentionDays;
    }

    public void setEventRetentionDays(Integer eventRetentionDays) {
        this.eventRetentionDays = eventRetentionDays;
    }

    public Integer getAlertRetentionDays() {
        return alertRetentionDays;
    }

    public void setAlertRetentionDays(Integer alertRetentionDays) {
        this.alertRetentionDays = alertRetentionDays;
    }

    public Integer getPredictionRetentionDays() {
        return predictionRetentionDays;
    }

    public void setPredictionRetentionDays(Integer predictionRetentionDays) {
        this.predictionRetentionDays = predictionRetentionDays;
    }

    /** Create default quota for a new tenant. */
    public static TenantQuota createDefault(UUID tenantId) {
        TenantQuota quota = new TenantQuota(tenantId);
        // Defaults are already set in field initializers
        return quota;
    }
}
