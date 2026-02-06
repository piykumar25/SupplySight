CREATE TABLE IF NOT EXISTS tracking.shipment_updates (
    id VARCHAR(255) PRIMARY KEY,
    tracking_number VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    description VARCHAR(255)
);

CREATE INDEX idx_shipment_updates_tracking_number ON tracking.shipment_updates(tracking_number);
