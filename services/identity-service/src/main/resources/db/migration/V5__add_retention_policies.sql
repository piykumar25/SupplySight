-- Add retention policies table
CREATE TABLE IF NOT EXISTS identity.retention_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE REFERENCES identity.tenants(id),
    event_retention_days INT NOT NULL DEFAULT 90,
    alert_retention_days INT NOT NULL DEFAULT 30,
    shipment_retention_days INT NOT NULL DEFAULT 365,
    audit_log_retention_days INT NOT NULL DEFAULT 730,
    soft_delete_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    soft_delete_grace_days INT NOT NULL DEFAULT 30,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create default retention policies for existing tenants
INSERT INTO identity.retention_policies (tenant_id)
SELECT id FROM identity.tenants
WHERE id NOT IN (SELECT tenant_id FROM identity.retention_policies);

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_retention_policies_tenant_id ON identity.retention_policies(tenant_id);
