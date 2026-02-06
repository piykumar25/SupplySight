CREATE TABLE IF NOT EXISTS tracking.shipments (
    id VARCHAR(255) PRIMARY KEY,
    tracking_number VARCHAR(255) NOT NULL UNIQUE,
    origin VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    current_status VARCHAR(255),
    estimated_delivery TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_shipments_tracking_number ON tracking.shipments(tracking_number);
