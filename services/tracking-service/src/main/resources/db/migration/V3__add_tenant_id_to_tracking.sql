-- Add tenant_id to shipments table
ALTER TABLE tracking.shipments
ADD COLUMN tenant_id UUID;

-- Update existing rows with a default tenant (if any exists) or a placeholder
-- For development, we can use a random UUID or null if we don't enforce NOT NULL immediately
-- But strict multi-tenancy requires NOT NULL.
-- CAUTION: In production, this would require a backfill strategy.
-- Here we will just set it to a random UUID for existing rows to satisfy the NOT NULL constraint if there is data.
-- Ideally, this should come from a known default tenant.
UPDATE tracking.shipments SET tenant_id = '00000000-0000-0000-0000-000000000000' WHERE tenant_id IS NULL;

ALTER TABLE tracking.shipments
ALTER COLUMN tenant_id SET NOT NULL;

-- Add index for performance
CREATE INDEX idx_shipments_tenant_id ON tracking.shipments(tenant_id);
CREATE INDEX idx_shipments_tenant_tracking ON tracking.shipments(tenant_id, tracking_number);


-- Add tenant_id to shipment_updates table
ALTER TABLE tracking.shipment_updates
ADD COLUMN tenant_id UUID;

UPDATE tracking.shipment_updates SET tenant_id = '00000000-0000-0000-0000-000000000000' WHERE tenant_id IS NULL;

ALTER TABLE tracking.shipment_updates
ALTER COLUMN tenant_id SET NOT NULL;

-- Add index for performance
CREATE INDEX idx_shipment_updates_tenant_id ON tracking.shipment_updates(tenant_id);
