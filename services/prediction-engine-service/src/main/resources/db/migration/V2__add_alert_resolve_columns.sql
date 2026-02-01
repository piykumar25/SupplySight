-- Add resolve columns to alerts table
ALTER TABLE prediction.alerts 
    ADD COLUMN IF NOT EXISTS resolved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS resolved_by UUID,
    ADD COLUMN IF NOT EXISTS resolution_comment TEXT;

-- Create index for resolved status
CREATE INDEX IF NOT EXISTS idx_alerts_tenant_resolved ON prediction.alerts(tenant_id, resolved);

-- Create composite index for common queries
CREATE INDEX IF NOT EXISTS idx_alerts_status_combined 
    ON prediction.alerts(tenant_id, acknowledged, resolved, created_at DESC);

-- Create index for severity filtering
CREATE INDEX IF NOT EXISTS idx_alerts_tenant_severity ON prediction.alerts(tenant_id, severity);

-- Add comment for new columns
COMMENT ON COLUMN prediction.alerts.resolved IS 'Whether the alert has been resolved';
COMMENT ON COLUMN prediction.alerts.resolved_at IS 'Timestamp when alert was resolved';
COMMENT ON COLUMN prediction.alerts.resolved_by IS 'User ID who resolved the alert';
COMMENT ON COLUMN prediction.alerts.resolution_comment IS 'Comment added when resolving the alert';
