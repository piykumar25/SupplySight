/**
 * Alerts API Client
 * Handles all API calls to the prediction service alerts endpoints
 */

import { predictionApi, apiRequest } from './axios';
import type { ApiResponse, PaginatedResponse } from '@/types/api.types';
import type {
    Alert,
    AlertSummary,
    AlertCount,
    AlertFilters
} from '@/features/alerts/types';

/**
 * API Parameters for listing alerts
 */
export interface ListAlertsParams {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: 'asc' | 'desc';
    unacknowledgedOnly?: boolean;
    severity?: string;
    alertType?: string;
    fromDate?: string;
    toDate?: string;
}

/**
 * Acknowledge alert request
 */
export interface AcknowledgeAlertRequest {
    userId: string;
    comment?: string;
}

/**
 * Resolve alert request
 */
export interface ResolveAlertRequest {
    userId: string;
    resolutionComment?: string;
}

/**
 * Add note request
 */
export interface AddNoteRequest {
    userId: string;
    note: string;
}

/**
 * Alerts API client
 */
export const alertsApi = {
    /**
     * List alerts with pagination and filters
     */
    async getAlerts(params: ListAlertsParams = {}): Promise<ApiResponse<PaginatedResponse<AlertSummary>>> {
        const response = await predictionApi.get<ApiResponse<PaginatedResponse<AlertSummary>>>('/alerts', {
            params: {
                page: params.page ?? 0,
                size: params.size ?? 20,
                sortBy: params.sortBy,
                sortDir: params.sortDir,
                unacknowledgedOnly: params.unacknowledgedOnly ?? false,
                severity: params.severity,
                alertType: params.alertType,
                fromDate: params.fromDate,
                toDate: params.toDate,
            },
        });
        return response.data;
    },

    /**
     * Get a single alert by ID
     */
    async getAlertById(alertId: string): Promise<Alert> {
        return apiRequest<Alert>(
            predictionApi.get<ApiResponse<Alert>>(`/alerts/${alertId}`)
        );
    },

    /**
     * Get alerts for a specific shipment
     */
    async getShipmentAlerts(shipmentId: string): Promise<AlertSummary[]> {
        return apiRequest<AlertSummary[]>(
            predictionApi.get<ApiResponse<AlertSummary[]>>(`/alerts/shipment/${shipmentId}`)
        );
    },

    /**
     * Get alert counts (total and unacknowledged)
     */
    async getAlertCount(): Promise<AlertCount> {
        return apiRequest<AlertCount>(
            predictionApi.get<ApiResponse<AlertCount>>('/alerts/count')
        );
    },

    /**
     * Acknowledge an alert
     */
    async acknowledgeAlert(alertId: string, request?: AcknowledgeAlertRequest): Promise<Alert> {
        return apiRequest<Alert>(
            predictionApi.post<ApiResponse<Alert>>(`/alerts/${alertId}/acknowledge`, request)
        );
    },

    /**
     * Resolve an alert
     */
    async resolveAlert(alertId: string, request?: ResolveAlertRequest): Promise<Alert> {
        return apiRequest<Alert>(
            predictionApi.post<ApiResponse<Alert>>(`/alerts/${alertId}/resolve`, request)
        );
    },

    /**
     * Bulk acknowledge alerts
     */
    async bulkAcknowledge(alertIds: string[], userId: string): Promise<{ success: number; failed: number }> {
        return apiRequest<{ success: number; failed: number }>(
            predictionApi.post<ApiResponse<{ success: number; failed: number }>>('/alerts/bulk/acknowledge', {
                alertIds,
                userId,
            })
        );
    },

    /**
     * Bulk resolve alerts
     */
    async bulkResolve(alertIds: string[], userId: string, comment?: string): Promise<{ success: number; failed: number }> {
        return apiRequest<{ success: number; failed: number }>(
            predictionApi.post<ApiResponse<{ success: number; failed: number }>>('/alerts/bulk/resolve', {
                alertIds,
                userId,
                resolutionComment: comment,
            })
        );
    },
};

/**
 * Build API params from AlertFilters
 */
export function buildAlertApiParams(
    filters: AlertFilters,
    page: number,
    size: number,
    sortBy: string,
    sortDir: 'asc' | 'desc'
): ListAlertsParams {
    const params: ListAlertsParams = {
        page,
        size,
        sortBy,
        sortDir,
    };

    // Severity filter - backend accepts single value
    if (filters.severities.length === 1) {
        params.severity = filters.severities[0];
    }

    // Alert type filter
    if (filters.alertTypes.length === 1) {
        params.alertType = filters.alertTypes[0];
    }

    // Status filter - map to unacknowledgedOnly
    if (filters.statuses.length === 1 && filters.statuses[0] === 'OPEN') {
        params.unacknowledgedOnly = true;
    }

    // Date range
    if (filters.fromDate) {
        params.fromDate = filters.fromDate;
    }
    if (filters.toDate) {
        params.toDate = filters.toDate;
    }

    return params;
}

export default alertsApi;
