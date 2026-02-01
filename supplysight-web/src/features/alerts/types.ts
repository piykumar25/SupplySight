/**
 * Alert Types for the Alerts & Exceptions feature
 * Matches backend DTOs from prediction-engine-service
 */

/**
 * Alert type enum matching backend Alert.AlertType
 */
export type AlertType =
    | 'DELAY_RISK_HIGH'
    | 'ANOMALY_DETECTED'
    | 'ETA_SLIP'
    | 'EXCESSIVE_DWELL'
    | 'SPEED_ANOMALY'
    | 'ROUTE_DEVIATION';

/**
 * Alert severity enum matching backend Alert.Severity
 */
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

/**
 * Alert status for UI state management
 */
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';

/**
 * Alert detail fields (optional additional data)
 */
export interface AlertDetails {
    location?: {
        lat: number;
        lng: number;
    };
    speed?: number;
    [key: string]: unknown;
}

/**
 * Full alert response from backend
 */
export interface Alert {
    id: string;
    shipmentId: string;
    tenantId: string;
    predictionId: string;
    alertType: AlertType;
    severity: AlertSeverity;
    message: string;
    details: AlertDetails;
    acknowledged: boolean;
    acknowledgedAt: string | null;
    acknowledgedBy: string | null;
    resolved?: boolean;
    resolvedAt?: string | null;
    resolvedBy?: string | null;
    resolutionComment?: string | null;
    createdAt: string;
}

/**
 * Alert summary for list views (lighter payload)
 */
export interface AlertSummary {
    id: string;
    shipmentId: string;
    alertType: AlertType;
    severity: AlertSeverity;
    message: string;
    acknowledged: boolean;
    createdAt: string;
}

/**
 * Alert count response
 */
export interface AlertCount {
    total: number;
    unacknowledged: number;
}

/**
 * SSE event types
 */
export type AlertStreamEventType = 'alert:new' | 'alert:updated' | 'alert:acknowledged' | 'alert:resolved';

/**
 * SSE event payload
 */
export interface AlertStreamEvent {
    type: AlertStreamEventType;
    data: AlertSummary;
    timestamp: string;
}

/**
 * Connection status for stream
 */
export type ConnectionStatus = 'connecting' | 'connected' | 'reconnecting' | 'disconnected' | 'error';

/**
 * Filter state for alerts list
 */
export interface AlertFilters {
    severities: AlertSeverity[];
    statuses: AlertStatus[];
    alertTypes: AlertType[];
    fromDate: string | null;
    toDate: string | null;
    search: string;
}

/**
 * Sortable column keys
 */
export type AlertSortColumn = 'severity' | 'createdAt' | 'alertType' | 'status';

/**
 * Sort direction
 */
export type SortDirection = 'asc' | 'desc';

/**
 * Sort configuration
 */
export interface AlertSortConfig {
    column: AlertSortColumn;
    direction: SortDirection;
}

/**
 * Pagination state
 */
export interface AlertPagination {
    page: number;
    size: number;
}

/**
 * Default filter values
 */
export const DEFAULT_ALERT_FILTERS: AlertFilters = {
    severities: [],
    statuses: [],
    alertTypes: [],
    fromDate: null,
    toDate: null,
    search: '',
};

/**
 * Default sort config
 */
export const DEFAULT_ALERT_SORT: AlertSortConfig = {
    column: 'createdAt',
    direction: 'desc',
};

/**
 * Default pagination
 */
export const DEFAULT_ALERT_PAGINATION: AlertPagination = {
    page: 0,
    size: 25,
};

/**
 * Severity display options
 */
export const SEVERITY_OPTIONS: { value: AlertSeverity; label: string; color: string }[] = [
    { value: 'CRITICAL', label: 'Critical', color: 'red' },
    { value: 'WARNING', label: 'Warning', color: 'yellow' },
    { value: 'INFO', label: 'Info', color: 'blue' },
];

/**
 * Status display options
 */
export const STATUS_OPTIONS: { value: AlertStatus; label: string }[] = [
    { value: 'OPEN', label: 'Open' },
    { value: 'ACKNOWLEDGED', label: 'Acknowledged' },
    { value: 'RESOLVED', label: 'Resolved' },
];

/**
 * Alert type display options
 */
export const ALERT_TYPE_OPTIONS: { value: AlertType; label: string }[] = [
    { value: 'DELAY_RISK_HIGH', label: 'High Delay Risk' },
    { value: 'ANOMALY_DETECTED', label: 'Anomaly Detected' },
    { value: 'ETA_SLIP', label: 'ETA Slip' },
    { value: 'EXCESSIVE_DWELL', label: 'Excessive Dwell' },
    { value: 'SPEED_ANOMALY', label: 'Speed Anomaly' },
    { value: 'ROUTE_DEVIATION', label: 'Route Deviation' },
];

/**
 * High severity types that trigger toasts
 */
export const HIGH_SEVERITY_TYPES: AlertType[] = ['DELAY_RISK_HIGH', 'ANOMALY_DETECTED'];

/**
 * Check if an alert is high severity (should trigger toast)
 */
export function isHighSeverityAlert(alert: AlertSummary): boolean {
    return HIGH_SEVERITY_TYPES.includes(alert.alertType) || alert.severity === 'CRITICAL';
}

/**
 * Get status from alert flags
 */
export function getAlertStatus(alert: Alert | AlertSummary): AlertStatus {
    if ('resolved' in alert && alert.resolved) return 'RESOLVED';
    if (alert.acknowledged) return 'ACKNOWLEDGED';
    return 'OPEN';
}

/**
 * Check if filters are active
 */
export function hasActiveAlertFilters(filters: AlertFilters): boolean {
    return (
        filters.severities.length > 0 ||
        filters.statuses.length > 0 ||
        filters.alertTypes.length > 0 ||
        filters.fromDate !== null ||
        filters.toDate !== null ||
        filters.search !== ''
    );
}
