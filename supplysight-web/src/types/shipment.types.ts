export type ShipmentStatus = 'CREATED' | 'PICKED_UP' | 'IN_TRANSIT' | 'DELIVERED' | 'EXCEPTION' | 'DELAYED';

export interface Location {
    lat: number;
    lon: number;
    address?: string;
    hubCode?: string;
}

/**
 * Shipment summary for list views (matches ShipmentSummary from backend)
 */
export interface Shipment {
    shipmentId: string;
    trackingNumber?: string;
    tenantId?: string;
    status: ShipmentStatus;
    origin?: Location;
    destination?: Location;
    expectedDeliveryDate?: string;
    actualDeliveryDate?: string;
    cargoDescription?: string;
    weight?: number;
    weightUnit?: string;
    lastEventTime?: string;
    lastEventType?: string;
    lastLocation?: Location;
    eta?: string;
    delayProbability?: number;
    updatedAt?: string;
}

/**
 * Full shipment detail (matches CurrentStateResponse from backend)
 */
export interface ShipmentDetail {
    shipmentId: string;
    tenantId: string;
    status: string;
    lastEventId?: string;
    lastEventTime?: string;
    lastEventType?: string;
    lastLocation?: Location;
    origin?: Location;
    destination?: Location;
    eta?: string;
    delayProbability?: number;
    eventCount?: number;
    createdAt?: string;
    updatedAt?: string;
}

export interface ShipmentEvent {
    eventId: string;
    shipmentId: string;
    eventType: string;
    eventTime: string;
    location?: Location;
    source: string;
    payload?: Record<string, any>;
}

export interface PredictionResult {
    shipmentId: string;
    tenantId: string;
    eta: string;
    etaConfidence: number;
    delayProbability: number;
    delayRisk: 'LOW' | 'MEDIUM' | 'HIGH';
    anomalyDetected: boolean;
    anomalyFlags: string[];
    factors: {
        distanceRemaining: number;
        averageSpeed: number;
        historicalOnTime: number;
        weatherImpact: number;
    };
    updatedAt: string;
}

