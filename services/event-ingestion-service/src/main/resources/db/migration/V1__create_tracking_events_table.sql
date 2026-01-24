-- Create tracking schema
CREATE SCHEMA IF NOT EXISTS tracking;

-- Tracking events table (immutable event store)
CREATE TABLE tracking.tracking_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    shipment_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(100) NOT NULL,
    location_lat DOUBLE PRECISION,
    location_lon DOUBLE PRECISION,
    location_hub_code VARCHAR(50),
    payload JSONB,
    metadata JSONB,
    ingested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id UUID,
    source_ip VARCHAR(50),
    CONSTRAINT uk_tracking_events_event_id UNIQUE (event_id)
);

-- Indexes for efficient querying
CREATE INDEX idx_tracking_events_tenant_shipment ON tracking.tracking_events(tenant_id, shipment_id);
CREATE INDEX idx_tracking_events_tenant_event_time ON tracking.tracking_events(tenant_id, event_time);
CREATE INDEX idx_tracking_events_shipment_event_time ON tracking.tracking_events(shipment_id, event_time);
CREATE INDEX idx_tracking_events_event_type ON tracking.tracking_events(event_type);
CREATE INDEX idx_tracking_events_ingested_at ON tracking.tracking_events(ingested_at);

-- Comments
COMMENT ON TABLE tracking.tracking_events IS 'Immutable event store for all tracking events';
COMMENT ON COLUMN tracking.tracking_events.event_id IS 'Unique event identifier for deduplication';
COMMENT ON COLUMN tracking.tracking_events.event_time IS 'Time when the event actually occurred';
COMMENT ON COLUMN tracking.tracking_events.ingested_at IS 'Time when the event was ingested into the system';
