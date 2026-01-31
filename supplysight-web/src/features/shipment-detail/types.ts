import { ShipmentStatus, Location, ShipmentEvent, PredictionResult } from '@/types/shipment.types';

/**
 * Exceptional event types that should be highlighted
 */
export const EXCEPTIONAL_EVENT_TYPES = [
    'CUSTOMS_HOLD',
    'DELAYED',
    'CANCELLED',
    'EXCEPTION',
    'DAMAGED',
    'LOST',
    'RETURNED',
] as const;

export type ExceptionalEventType = (typeof EXCEPTIONAL_EVENT_TYPES)[number];

/**
 * Check if an event type is exceptional
 */
export function isExceptionalEvent(eventType: string): boolean {
    return EXCEPTIONAL_EVENT_TYPES.includes(eventType as ExceptionalEventType);
}

/**
 * Risk level derived from delay probability
 */
export type RiskLevel = 'normal' | 'delayed' | 'high';

export function getRiskLevel(delayProbability: number | undefined): RiskLevel {
    if (delayProbability === undefined) return 'normal';
    if (delayProbability > 0.8) return 'high';
    if (delayProbability > 0.5) return 'delayed';
    return 'normal';
}

/**
 * Risk level display configuration
 */
export const RISK_CONFIG: Record<RiskLevel, { label: string; color: string; bgColor: string }> = {
    normal: { label: 'Normal', color: 'text-green-400', bgColor: 'bg-green-500/20' },
    delayed: { label: 'Delayed', color: 'text-yellow-400', bgColor: 'bg-yellow-500/20' },
    high: { label: 'High Risk', color: 'text-red-400', bgColor: 'bg-red-500/20' },
};

/**
 * Status display configuration
 */
export const STATUS_CONFIG: Record<ShipmentStatus, { label: string; color: string; bgColor: string }> = {
    CREATED: { label: 'Created', color: 'text-gray-400', bgColor: 'bg-gray-500/20' },
    PICKED_UP: { label: 'Picked Up', color: 'text-blue-400', bgColor: 'bg-blue-500/20' },
    IN_TRANSIT: { label: 'In Transit', color: 'text-blue-300', bgColor: 'bg-blue-600/20' },
    DELIVERED: { label: 'Delivered', color: 'text-green-400', bgColor: 'bg-green-500/20' },
    EXCEPTION: { label: 'Exception', color: 'text-red-400', bgColor: 'bg-red-500/20' },
    DELAYED: { label: 'Delayed', color: 'text-yellow-400', bgColor: 'bg-yellow-500/20' },
};

/**
 * Event type icons/colors
 */
export const EVENT_TYPE_CONFIG: Record<string, { icon: string; color: string }> = {
    CREATED: { icon: '📦', color: 'text-gray-400' },
    PICKED_UP: { icon: '🚚', color: 'text-blue-400' },
    DEPARTED: { icon: '🛫', color: 'text-blue-400' },
    ARRIVED: { icon: '🛬', color: 'text-blue-400' },
    IN_TRANSIT: { icon: '🚛', color: 'text-blue-300' },
    OUT_FOR_DELIVERY: { icon: '📬', color: 'text-blue-300' },
    DELIVERED: { icon: '✅', color: 'text-green-400' },
    CUSTOMS_HOLD: { icon: '🛃', color: 'text-yellow-400' },
    DELAYED: { icon: '⏰', color: 'text-yellow-400' },
    EXCEPTION: { icon: '⚠️', color: 'text-red-400' },
    CANCELLED: { icon: '❌', color: 'text-red-400' },
    LOCATION_UPDATE: { icon: '📍', color: 'text-gray-400' },
};

export function getEventConfig(eventType: string) {
    return EVENT_TYPE_CONFIG[eventType] || { icon: '•', color: 'text-gray-400' };
}

/**
 * Sort events by eventTime (chronological)
 */
export function sortEventsByTime(events: ShipmentEvent[]): ShipmentEvent[] {
    return [...events].sort(
        (a, b) => new Date(a.eventTime).getTime() - new Date(b.eventTime).getTime()
    );
}

/**
 * Group events by date
 */
export interface EventGroup {
    date: string;
    dateLabel: string;
    events: ShipmentEvent[];
}

export function groupEventsByDate(events: ShipmentEvent[]): EventGroup[] {
    const sorted = sortEventsByTime(events);
    const groups: Map<string, ShipmentEvent[]> = new Map();

    sorted.forEach((event) => {
        const date = new Date(event.eventTime).toLocaleDateString();
        const existing = groups.get(date) || [];
        groups.set(date, [...existing, event]);
    });

    return Array.from(groups.entries()).map(([date, events]) => ({
        date,
        dateLabel: formatDateLabel(new Date(events[0].eventTime)),
        events,
    }));
}

function formatDateLabel(date: Date): string {
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
        return 'Today';
    }
    if (date.toDateString() === yesterday.toDateString()) {
        return 'Yesterday';
    }
    return date.toLocaleDateString('en-US', {
        weekday: 'long',
        month: 'short',
        day: 'numeric',
    });
}

/**
 * Replay mode state
 */
export interface ReplayState {
    isPlaying: boolean;
    currentIndex: number;
    speed: number; // ms between events
}

export const DEFAULT_REPLAY_STATE: ReplayState = {
    isPlaying: false,
    currentIndex: 0,
    speed: 1000,
};

/**
 * Extract locations from timeline for map
 */
export function extractLocationsFromTimeline(
    events: ShipmentEvent[],
    origin?: Location,
    destination?: Location
): { locations: Location[]; current?: Location } {
    const sorted = sortEventsByTime(events);
    const locations: Location[] = [];

    // Add origin first
    if (origin?.lat && origin?.lon) {
        locations.push(origin);
    }

    // Add event locations
    sorted.forEach((event) => {
        if (event.location?.lat && event.location?.lon) {
            locations.push(event.location);
        }
    });

    // Current is the last known location
    const current = locations[locations.length - 1];

    // Add destination (may not be reached yet)
    if (destination?.lat && destination?.lon) {
        // Don't add destination to path unless delivered
        // Just keep it for marker
    }

    return { locations, current };
}

/**
 * Format prediction factors into explanation
 */
export function formatPredictionExplanation(prediction: PredictionResult): string[] {
    const explanations: string[] = [];
    const { factors, anomalyFlags, delayProbability } = prediction;

    if (delayProbability > 0.8) {
        explanations.push('High risk of delay detected');
    }

    if (factors.weatherImpact > 0.5) {
        explanations.push(`Weather impact: ${(factors.weatherImpact * 100).toFixed(0)}% slowdown expected`);
    }

    if (factors.historicalOnTime < 0.7) {
        explanations.push(`Route historically on-time only ${(factors.historicalOnTime * 100).toFixed(0)}% of deliveries`);
    }

    if (factors.averageSpeed < 30) {
        explanations.push(`Low average speed: ${factors.averageSpeed.toFixed(0)} km/h`);
    }

    if (factors.distanceRemaining > 500) {
        explanations.push(`${factors.distanceRemaining.toFixed(0)} km remaining`);
    }

    anomalyFlags.forEach((flag) => {
        explanations.push(`Anomaly: ${flag.replace(/_/g, ' ').toLowerCase()}`);
    });

    return explanations.length > 0 ? explanations : ['No significant concerns'];
}
