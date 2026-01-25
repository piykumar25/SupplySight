/**
 * Application-wide constants
 */

// API base URLs for different services
export const API_BASE_URLS = {
    IDENTITY: import.meta.env.VITE_IDENTITY_API_URL || 'http://localhost:8081/api/v1',
    TRACKING: import.meta.env.VITE_TRACKING_API_URL || 'http://localhost:8083/api/v1',
    VISIBILITY: import.meta.env.VITE_VISIBILITY_API_URL || 'http://localhost:8084/api/v1',
    PREDICTION: import.meta.env.VITE_PREDICTION_API_URL || 'http://localhost:8085/api/v1',
} as const;

// Storage keys
export const STORAGE_KEYS = {
    REFRESH_TOKEN: 'supplysight_refresh_token',
    USER: 'supplysight_user',
} as const;

// Route paths
export const ROUTES = {
    LOGIN: '/login',
    DASHBOARD: '/',
    SHIPMENTS: '/shipments',
    SHIPMENT_DETAIL: '/shipments/:id',
    ALERTS: '/alerts',
    SETTINGS: '/settings',
    UNAUTHORIZED: '/unauthorized',
} as const;

// HTTP headers
export const HEADERS = {
    CORRELATION_ID: 'X-Correlation-ID',
    TENANT_ID: 'X-Tenant-ID',
} as const;
