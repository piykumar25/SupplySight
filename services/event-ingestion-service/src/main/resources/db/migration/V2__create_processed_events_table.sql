-- Processed events table for idempotency/deduplication
CREATE TABLE tracking.processed_events (
    event_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSED',
    source VARCHAR(50) NOT NULL DEFAULT 'REST'
);

-- Index for cleanup queries
CREATE INDEX idx_processed_events_processed_at ON tracking.processed_events(processed_at);
CREATE INDEX idx_processed_events_tenant ON tracking.processed_events(tenant_id);

-- Cleanup function for old processed event records (keep last 7 days)
CREATE OR REPLACE FUNCTION tracking.cleanup_processed_events(retention_days INTEGER DEFAULT 7)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM tracking.processed_events 
    WHERE processed_at < CURRENT_TIMESTAMP - (retention_days || ' days')::INTERVAL;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Comments
COMMENT ON TABLE tracking.processed_events IS 'Tracks processed event IDs for deduplication';
COMMENT ON COLUMN tracking.processed_events.source IS 'Source of the event: REST, KAFKA_RAW';
