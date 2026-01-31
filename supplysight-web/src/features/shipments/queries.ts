import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { visibilityClient } from '@/api';
import { useAuth } from '@/store';
import { PaginatedResponse } from '@/types/api.types';
import { Shipment } from '@/types/shipment.types';
import {
    ShipmentFilters,
    ShipmentSortConfig,
    ShipmentPagination,
    getRiskLevel,
} from './types';

/**
 * Query key factory for shipments
 */
export const shipmentKeys = {
    all: ['shipments'] as const,
    lists: () => [...shipmentKeys.all, 'list'] as const,
    list: (params: ShipmentQueryConfig) => [...shipmentKeys.lists(), params] as const,
    details: () => [...shipmentKeys.all, 'detail'] as const,
    detail: (id: string) => [...shipmentKeys.details(), id] as const,
};

/**
 * Combined query config
 */
export interface ShipmentQueryConfig {
    filters: ShipmentFilters;
    sort: ShipmentSortConfig;
    pagination: ShipmentPagination;
}

/**
 * API params that get sent to the backend
 */
interface ApiParams {
    page: number;
    size: number;
    sortBy: string;
    sortDir: 'asc' | 'desc';
    status?: string;
    fromDate?: string;
    toDate?: string;
}

/**
 * Build API params from query config
 * Only includes params that the backend supports
 */
function buildApiParams(config: ShipmentQueryConfig): ApiParams {
    const { filters, sort, pagination } = config;

    const params: ApiParams = {
        page: pagination.page,
        size: pagination.size,
        sortBy: sort.column,
        sortDir: sort.direction,
    };

    // Status filter - backend only supports single status currently
    // If multiple statuses selected, we filter client-side
    if (filters.statuses.length === 1) {
        params.status = filters.statuses[0];
    }

    // Date range filters
    if (filters.fromDate) {
        params.fromDate = filters.fromDate;
    }
    if (filters.toDate) {
        params.toDate = filters.toDate;
    }

    return params;
}

/**
 * Apply client-side filters that backend doesn't support
 */
function applyClientFilters(
    shipments: Shipment[],
    filters: ShipmentFilters
): Shipment[] {
    let result = shipments;

    // Multi-status filter (if more than 1 status selected)
    if (filters.statuses.length > 1) {
        result = result.filter((s) => filters.statuses.includes(s.status));
    }

    // Origin filter
    if (filters.origin) {
        const originLower = filters.origin.toLowerCase();
        result = result.filter(
            (s) =>
                s.origin?.hubCode?.toLowerCase().includes(originLower) ||
                s.origin?.address?.toLowerCase().includes(originLower)
        );
    }

    // Destination filter
    if (filters.destination) {
        const destLower = filters.destination.toLowerCase();
        result = result.filter(
            (s) =>
                s.destination?.hubCode?.toLowerCase().includes(destLower) ||
                s.destination?.address?.toLowerCase().includes(destLower)
        );
    }

    // Risk level filter
    if (filters.riskLevel !== 'all') {
        result = result.filter((s) => getRiskLevel(s.delayProbability) === filters.riskLevel);
    }

    // Free-text search (tracking number, shipment ID)
    if (filters.search) {
        const searchLower = filters.search.toLowerCase();
        result = result.filter(
            (s) =>
                s.trackingNumber?.toLowerCase().includes(searchLower) ||
                s.shipmentId?.toLowerCase().includes(searchLower)
        );
    }

    return result;
}

/**
 * Main query hook for shipments list
 */
export function useShipmentsQuery(config: ShipmentQueryConfig) {
    const { isAuthenticated } = useAuth();
    const apiParams = buildApiParams(config);

    return useQuery<PaginatedResponse<Shipment>>({
        queryKey: shipmentKeys.list(config),
        queryFn: async () => {
            const result = await visibilityClient.getShipments(apiParams);
            if (!result.success) {
                throw new Error('Failed to fetch shipments');
            }

            // Apply client-side filters
            const filteredContent = applyClientFilters(result.data.content, config.filters);

            // Return with adjusted counts
            // Note: Client filtering may affect pagination accuracy
            return {
                ...result.data,
                content: filteredContent,
                // Keep original totalElements for server-filtered results
                // This is a known limitation - accurate counts require backend filtering
            };
        },
        enabled: isAuthenticated,
        placeholderData: keepPreviousData,
        staleTime: 30 * 1000, // 30 seconds
    });
}

/**
 * Check if we need client-side filtering
 */
export function needsClientFiltering(filters: ShipmentFilters): boolean {
    return (
        filters.statuses.length > 1 ||
        filters.origin !== '' ||
        filters.destination !== '' ||
        filters.riskLevel !== 'all' ||
        filters.search !== ''
    );
}
