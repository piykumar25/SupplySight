-- Tenant Quotas table for storing per-tenant limits and retention policies
CREATE TABLE identity.tenant_quotas (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE REFERENCES identity.tenants(id) ON DELETE CASCADE,
    max_active_shipments INTEGER NOT NULL DEFAULT 1000,
    max_events_per_second INTEGER NOT NULL DEFAULT 100,
    max_sse_connections INTEGER NOT NULL DEFAULT 50,
    event_retention_days INTEGER NOT NULL DEFAULT 90,
    alert_retention_days INTEGER NOT NULL DEFAULT 30,
    prediction_retention_days INTEGER NOT NULL DEFAULT 30,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT DEFAULT 0
);

-- Index for fast lookup by tenant
CREATE INDEX idx_tenant_quotas_tenant_id ON identity.tenant_quotas(tenant_id);

-- Add comment for documentation
COMMENT ON TABLE identity.tenant_quotas IS 'Per-tenant quotas and limits for resource consumption';
COMMENT ON COLUMN identity.tenant_quotas.max_active_shipments IS 'Maximum number of active shipments allowed';
COMMENT ON COLUMN identity.tenant_quotas.max_events_per_second IS 'Maximum events ingested per second';
COMMENT ON COLUMN identity.tenant_quotas.max_sse_connections IS 'Maximum concurrent SSE connections';
COMMENT ON COLUMN identity.tenant_quotas.event_retention_days IS 'Days to retain tracking events';
COMMENT ON COLUMN identity.tenant_quotas.alert_retention_days IS 'Days to retain alerts';
COMMENT ON COLUMN identity.tenant_quotas.prediction_retention_days IS 'Days to retain predictions';
