/**
 * Alerts Query Hooks
 * TanStack Query hooks for alerts data fetching and mutations
 */

import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { alertsApi, buildAlertApiParams } from '@/api/alerts.api';
import { useAuth } from '@/store';
import { useAlertsStore } from './store';
import type { PaginatedResponse } from '@/types/api.types';
import type {
    Alert,
    AlertSummary,
    AlertCount,
    AlertFilters,
    AlertSortConfig,
    AlertPagination
} from './types';

/**
 * Query key factory for alerts
 */
export const alertKeys = {
    all: ['alerts'] as const,
    lists: () => [...alertKeys.all, 'list'] as const,
    list: (params: AlertQueryConfig) => [...alertKeys.lists(), params] as const,
    details: () => [...alertKeys.all, 'detail'] as const,
    detail: (id: string) => [...alertKeys.details(), id] as const,
    counts: () => [...alertKeys.all, 'count'] as const,
    shipment: (shipmentId: string) => [...alertKeys.all, 'shipment', shipmentId] as const,
};

/**
 * Combined query configuration
 */
export interface AlertQueryConfig {
    filters: AlertFilters;
    sort: AlertSortConfig;
    pagination: AlertPagination;
}

/**
 * Hook to fetch paginated alerts list
 */
export function useAlertsQuery(config: AlertQueryConfig) {
    const { isAuthenticated } = useAuth();
    const apiParams = buildAlertApiParams(
        config.filters,
        config.pagination.page,
        config.pagination.size,
        config.sort.column,
        config.sort.direction
    );

    return useQuery<PaginatedResponse<AlertSummary>>({
        queryKey: alertKeys.list(config),
        queryFn: async () => {
            const result = await alertsApi.getAlerts(apiParams);
            if (!result.success) {
                throw new Error('Failed to fetch alerts');
            }

            // Apply client-side filters that backend doesn't support
            let content = result.data.content;

            // Multi-severity filter
            if (config.filters.severities.length > 1) {
                content = content.filter((a) => config.filters.severities.includes(a.severity));
            }

            // Multi-type filter
            if (config.filters.alertTypes.length > 1) {
                content = content.filter((a) => config.filters.alertTypes.includes(a.alertType));
            }

            // Free-text search
            if (config.filters.search) {
                const searchLower = config.filters.search.toLowerCase();
                content = content.filter(
                    (a) =>
                        a.message.toLowerCase().includes(searchLower) ||
                        a.id.toLowerCase().includes(searchLower) ||
                        a.shipmentId.toLowerCase().includes(searchLower)
                );
            }

            return {
                ...result.data,
                content,
            };
        },
        enabled: isAuthenticated,
        placeholderData: keepPreviousData,
        staleTime: 30 * 1000, // 30 seconds
    });
}

/**
 * Hook to fetch single alert detail
 */
export function useAlertDetailQuery(alertId: string | null) {
    const { isAuthenticated } = useAuth();

    return useQuery<Alert>({
        queryKey: alertKeys.detail(alertId ?? ''),
        queryFn: () => alertsApi.getAlertById(alertId!),
        enabled: isAuthenticated && !!alertId,
        staleTime: 60 * 1000, // 1 minute
    });
}

/**
 * Hook to fetch alert counts
 */
export function useAlertCountQuery() {
    const { isAuthenticated } = useAuth();
    const setUnreadCount = useAlertsStore((state) => state.setUnreadCount);
    const setTotalCount = useAlertsStore((state) => state.setTotalCount);

    return useQuery<AlertCount>({
        queryKey: alertKeys.counts(),
        queryFn: async () => {
            const count = await alertsApi.getAlertCount();
            // Sync to store for bell icon
            setUnreadCount(count.unacknowledged);
            setTotalCount(count.total);
            return count;
        },
        enabled: isAuthenticated,
        staleTime: 30 * 1000, // 30 seconds
        refetchInterval: 60 * 1000, // Refetch every minute
    });
}

/**
 * Hook to fetch alerts for a specific shipment
 */
export function useShipmentAlertsQuery(shipmentId: string | null) {
    const { isAuthenticated } = useAuth();

    return useQuery<AlertSummary[]>({
        queryKey: alertKeys.shipment(shipmentId ?? ''),
        queryFn: () => alertsApi.getShipmentAlerts(shipmentId!),
        enabled: isAuthenticated && !!shipmentId,
        staleTime: 30 * 1000,
    });
}

/**
 * Hook for acknowledging an alert
 */
export function useAcknowledgeAlertMutation() {
    const queryClient = useQueryClient();
    const { user } = useAuth();
    const decrementUnreadCount = useAlertsStore((state) => state.decrementUnreadCount);
    const updateRecentAlert = useAlertsStore((state) => state.updateRecentAlert);

    return useMutation({
        mutationFn: ({ alertId, comment }: { alertId: string; comment?: string }) =>
            alertsApi.acknowledgeAlert(alertId, {
                userId: user?.id ?? '',
                comment,
            }),
        onSuccess: (_data, variables) => {
            // Update cache
            queryClient.invalidateQueries({ queryKey: alertKeys.lists() });
            queryClient.invalidateQueries({ queryKey: alertKeys.detail(variables.alertId) });
            queryClient.invalidateQueries({ queryKey: alertKeys.counts() });

            // Update store
            decrementUnreadCount();
            updateRecentAlert(variables.alertId, { acknowledged: true });
        },
    });
}

/**
 * Hook for resolving an alert
 */
export function useResolveAlertMutation() {
    const queryClient = useQueryClient();
    const { user } = useAuth();
    const updateRecentAlert = useAlertsStore((state) => state.updateRecentAlert);

    return useMutation({
        mutationFn: ({ alertId, comment }: { alertId: string; comment?: string }) =>
            alertsApi.resolveAlert(alertId, {
                userId: user?.id ?? '',
                resolutionComment: comment,
            }),
        onSuccess: (_data, variables) => {
            // Update cache
            queryClient.invalidateQueries({ queryKey: alertKeys.lists() });
            queryClient.invalidateQueries({ queryKey: alertKeys.detail(variables.alertId) });
            queryClient.invalidateQueries({ queryKey: alertKeys.counts() });

            // Update store
            updateRecentAlert(variables.alertId, { acknowledged: true });
        },
    });
}

/**
 * Hook for bulk acknowledging alerts
 */
export function useBulkAcknowledgeMutation() {
    const queryClient = useQueryClient();
    const { user } = useAuth();

    return useMutation({
        mutationFn: (alertIds: string[]) =>
            alertsApi.bulkAcknowledge(alertIds, user?.id ?? ''),
        onSuccess: () => {
            // Invalidate all alert queries
            queryClient.invalidateQueries({ queryKey: alertKeys.all });
        },
    });
}

/**
 * Hook for bulk resolving alerts
 */
export function useBulkResolveMutation() {
    const queryClient = useQueryClient();
    const { user } = useAuth();

    return useMutation({
        mutationFn: ({ alertIds, comment }: { alertIds: string[]; comment?: string }) =>
            alertsApi.bulkResolve(alertIds, user?.id ?? '', comment),
        onSuccess: () => {
            // Invalidate all alert queries
            queryClient.invalidateQueries({ queryKey: alertKeys.all });
        },
    });
}

/**
 * Hook to prefetch alert count on app load
 */
export function usePrefetchAlertCount() {
    const queryClient = useQueryClient();
    const { isAuthenticated } = useAuth();

    if (isAuthenticated) {
        queryClient.prefetchQuery({
            queryKey: alertKeys.counts(),
            queryFn: () => alertsApi.getAlertCount(),
        });
    }
}
