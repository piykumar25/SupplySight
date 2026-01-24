-- Create prediction schema
CREATE SCHEMA IF NOT EXISTS prediction;

-- Shipment predictions table
CREATE TABLE prediction.shipment_predictions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    shipment_id UUID NOT NULL,
    event_id UUID NOT NULL,
    eta TIMESTAMP WITH TIME ZONE,
    eta_confidence DOUBLE PRECISION,
    delay_probability DOUBLE PRECISION NOT NULL,
    delay_risk VARCHAR(20) NOT NULL,
    anomaly_detected BOOLEAN NOT NULL DEFAULT FALSE,
    anomaly_flags JSONB DEFAULT '[]',
    distance_remaining_km DOUBLE PRECISION,
    average_speed_kmph DOUBLE PRECISION,
    dwell_time_hours DOUBLE PRECISION,
    factors JSONB,
    model_version VARCHAR(50) DEFAULT 'heuristic-v1',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prediction_shipment_event UNIQUE (shipment_id, event_id)
);

-- Indexes
CREATE INDEX idx_predictions_tenant_shipment ON prediction.shipment_predictions(tenant_id, shipment_id);
CREATE INDEX idx_predictions_tenant_created ON prediction.shipment_predictions(tenant_id, created_at);
CREATE INDEX idx_predictions_delay_risk ON prediction.shipment_predictions(tenant_id, delay_risk);
CREATE INDEX idx_predictions_anomaly ON prediction.shipment_predictions(tenant_id, anomaly_detected);

-- Alerts table (for high-risk and anomaly alerts)
CREATE TABLE prediction.alerts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    shipment_id UUID NOT NULL,
    prediction_id UUID NOT NULL REFERENCES prediction.shipment_predictions(id),
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    details JSONB,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for alerts
CREATE INDEX idx_alerts_tenant_shipment ON prediction.alerts(tenant_id, shipment_id);
CREATE INDEX idx_alerts_tenant_type ON prediction.alerts(tenant_id, alert_type);
CREATE INDEX idx_alerts_tenant_acknowledged ON prediction.alerts(tenant_id, acknowledged);
CREATE INDEX idx_alerts_created_at ON prediction.alerts(created_at);

-- Comments
COMMENT ON TABLE prediction.shipment_predictions IS 'Prediction results for shipments';
COMMENT ON TABLE prediction.alerts IS 'Alerts generated from predictions';
COMMENT ON COLUMN prediction.shipment_predictions.delay_risk IS 'LOW, MEDIUM, HIGH';
COMMENT ON COLUMN prediction.alerts.alert_type IS 'DELAY_RISK_HIGH, ANOMALY_DETECTED, ETA_SLIP';
COMMENT ON COLUMN prediction.alerts.severity IS 'INFO, WARNING, CRITICAL';
