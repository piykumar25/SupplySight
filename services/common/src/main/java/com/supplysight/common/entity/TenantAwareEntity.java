package com.supplysight.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import com.supplysight.common.security.TenantContext;

import java.util.UUID;

/**
 * Base entity for tenant-scoped entities.
 * Automatically sets tenantId from TenantContext on persist.
 */
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    protected TenantAwareEntity() {
        super();
    }

    protected TenantAwareEntity(UUID id) {
        super(id);
    }

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (this.tenantId == null) {
            UUID contextTenantId = TenantContext.getTenantId();
            if (contextTenantId != null) {
                this.tenantId = contextTenantId;
            }
        }
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
}
