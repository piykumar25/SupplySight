package com.supplysight.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity to store per-tenant data retention policies.
 */
@Entity
@Table(name = "retention_policies", schema = "identity")
public class RetentionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    /**
     * Days to retain shipment event data (default: 90 days)
     */
    @Column(name = "event_retention_days", nullable = false)
    private int eventRetentionDays = 90;

    /**
     * Days to retain alert data (default: 30 days)
     */
    @Column(name = "alert_retention_days", nullable = false)
    private int alertRetentionDays = 30;

    /**
     * Days to retain completed shipments (default: 365 days)
     */
    @Column(name = "shipment_retention_days", nullable = false)
    private int shipmentRetentionDays = 365;

    /**
     * Days to retain audit logs (default: 730 days = 2 years)
     */
    @Column(name = "audit_log_retention_days", nullable = false)
    private int auditLogRetentionDays = 730;

    /**
     * Whether to use soft delete (mark deleted) vs hard delete (remove data)
     */
    @Column(name = "soft_delete_enabled", nullable = false)
    private boolean softDeleteEnabled = true;

    /**
     * Days to retain soft-deleted records before permanent deletion
     */
    @Column(name = "soft_delete_grace_days", nullable = false)
    private int softDeleteGraceDays = 30;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public int getEventRetentionDays() {
        return eventRetentionDays;
    }

    public void setEventRetentionDays(int eventRetentionDays) {
        this.eventRetentionDays = eventRetentionDays;
    }

    public int getAlertRetentionDays() {
        return alertRetentionDays;
    }

    public void setAlertRetentionDays(int alertRetentionDays) {
        this.alertRetentionDays = alertRetentionDays;
    }

    public int getShipmentRetentionDays() {
        return shipmentRetentionDays;
    }

    public void setShipmentRetentionDays(int shipmentRetentionDays) {
        this.shipmentRetentionDays = shipmentRetentionDays;
    }

    public int getAuditLogRetentionDays() {
        return auditLogRetentionDays;
    }

    public void setAuditLogRetentionDays(int auditLogRetentionDays) {
        this.auditLogRetentionDays = auditLogRetentionDays;
    }

    public boolean isSoftDeleteEnabled() {
        return softDeleteEnabled;
    }

    public void setSoftDeleteEnabled(boolean softDeleteEnabled) {
        this.softDeleteEnabled = softDeleteEnabled;
    }

    public int getSoftDeleteGraceDays() {
        return softDeleteGraceDays;
    }

    public void setSoftDeleteGraceDays(int softDeleteGraceDays) {
        this.softDeleteGraceDays = softDeleteGraceDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
