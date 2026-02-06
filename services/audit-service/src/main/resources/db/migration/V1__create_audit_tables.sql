-- Audit schema for compliance logging
CREATE SCHEMA IF NOT EXISTS audit;

-- Audit log table - append-only for compliance
CREATE TABLE IF NOT EXISTS audit.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    tenant_id UUID,
    user_id UUID,
    username VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(255),
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    source_ip VARCHAR(45),
    user_agent TEXT,
    details JSONB,
    correlation_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Partition key for time-based partitioning
    partition_key DATE GENERATED ALWAYS AS (DATE(event_time AT TIME ZONE 'UTC')) STORED
);

-- Indexes for common queries
CREATE INDEX idx_audit_tenant_time ON audit.audit_logs(tenant_id, event_time DESC);
CREATE INDEX idx_audit_user_time ON audit.audit_logs(user_id, event_time DESC);
CREATE INDEX idx_audit_action ON audit.audit_logs(action, event_time DESC);
CREATE INDEX idx_audit_resource ON audit.audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_correlation ON audit.audit_logs(correlation_id);
CREATE INDEX idx_audit_event_time ON audit.audit_logs(event_time DESC);
CREATE INDEX idx_audit_partition ON audit.audit_logs(partition_key);

-- GIN index for JSONB details
CREATE INDEX idx_audit_details ON audit.audit_logs USING GIN (details);

-- Create a view for recent audit entries
CREATE OR REPLACE VIEW audit.recent_audit AS
SELECT * FROM audit.audit_logs
WHERE event_time > CURRENT_TIMESTAMP - INTERVAL '24 hours'
ORDER BY event_time DESC;

-- Function to prevent updates/deletes on audit_logs (immutability)
CREATE OR REPLACE FUNCTION audit.prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable and cannot be modified or deleted';
END;
$$ LANGUAGE plpgsql;

-- Trigger to enforce immutability
CREATE TRIGGER audit_logs_immutable
    BEFORE UPDATE OR DELETE ON audit.audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION audit.prevent_audit_modification();

-- Alert history table for tracking alert lifecycle
CREATE TABLE IF NOT EXISTS audit.alert_history (
    id BIGSERIAL PRIMARY KEY,
    alert_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    shipment_id UUID,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    acknowledged_by UUID,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    details JSONB
);

CREATE INDEX idx_alert_history_tenant ON audit.alert_history(tenant_id, created_at DESC);
CREATE INDEX idx_alert_history_status ON audit.alert_history(status, created_at DESC);
CREATE INDEX idx_alert_history_alert_id ON audit.alert_history(alert_id);

-- Summary statistics table for reporting
CREATE TABLE IF NOT EXISTS audit.audit_statistics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    stat_date DATE NOT NULL,
    action VARCHAR(100) NOT NULL,
    count BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(tenant_id, stat_date, action)
);

CREATE INDEX idx_audit_stats_tenant_date ON audit.audit_statistics(tenant_id, stat_date DESC);

-- Function to increment statistics
CREATE OR REPLACE FUNCTION audit.increment_statistics()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit.audit_statistics (tenant_id, stat_date, action, count)
    VALUES (NEW.tenant_id, DATE(NEW.event_time), NEW.action, 1)
    ON CONFLICT (tenant_id, stat_date, action)
    DO UPDATE SET count = audit.audit_statistics.count + 1,
                  updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update statistics
CREATE TRIGGER audit_log_stats_trigger
    AFTER INSERT ON audit.audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION audit.increment_statistics();

COMMENT ON TABLE audit.audit_logs IS 'Immutable audit trail for all system events';
COMMENT ON TABLE audit.alert_history IS 'History of alerts and their lifecycle';
COMMENT ON TABLE audit.audit_statistics IS 'Pre-aggregated statistics for reporting';
