-- Create visibility schema
CREATE SCHEMA IF NOT EXISTS visibility;

-- Shipment current state (materialized view)
CREATE TABLE visibility.shipment_current_state (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    shipment_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_event_id UUID NOT NULL,
    last_event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    last_event_type VARCHAR(50) NOT NULL,
    location_lat DOUBLE PRECISION,
    location_lon DOUBLE PRECISION,
    location_hub_code VARCHAR(50),
    origin_lat DOUBLE PRECISION,
    origin_lon DOUBLE PRECISION,
    destination_lat DOUBLE PRECISION,
    destination_lon DOUBLE PRECISION,
    eta TIMESTAMP WITH TIME ZONE,
    delay_probability DOUBLE PRECISION,
    event_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_shipment_current_state_tenant_shipment UNIQUE (tenant_id, shipment_id)
);

-- Indexes for efficient querying
CREATE INDEX idx_shipment_current_state_tenant_id ON visibility.shipment_current_state(tenant_id);
CREATE INDEX idx_shipment_current_state_status ON visibility.shipment_current_state(tenant_id, status);
CREATE INDEX idx_shipment_current_state_updated_at ON visibility.shipment_current_state(tenant_id, updated_at);
CREATE INDEX idx_shipment_current_state_last_event_time ON visibility.shipment_current_state(tenant_id, last_event_time);

-- Shipment timeline (immutable event log)
CREATE TABLE visibility.shipment_timeline (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    shipment_id UUID NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(100) NOT NULL,
    location_lat DOUBLE PRECISION,
    location_lon DOUBLE PRECISION,
    location_hub_code VARCHAR(50),
    payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_shipment_timeline_event UNIQUE (event_id)
);

-- Indexes for timeline queries
CREATE INDEX idx_shipment_timeline_tenant_shipment ON visibility.shipment_timeline(tenant_id, shipment_id);
CREATE INDEX idx_shipment_timeline_event_time ON visibility.shipment_timeline(shipment_id, event_time);

-- Comments
COMMENT ON TABLE visibility.shipment_current_state IS 'Materialized view of current shipment state for fast lookups';
COMMENT ON TABLE visibility.shipment_timeline IS 'Immutable timeline of all events for a shipment';
COMMENT ON COLUMN visibility.shipment_current_state.last_event_time IS 'Event time used for out-of-order handling';
