import { useQuery } from '@tanstack/react-query';
import { visibilityClient } from '@/api';
import { useAuth } from '@/store';
import { Shipment } from '@/types/shipment.types';
import { dashboardQueryKeys, DASHBOARD_REFRESH_INTERVAL } from '../queries/dashboard.queries';

export interface DashboardStats {
    activeShipments: number;
    delayedShipments: number;
    onTimePercentage: number;
    highRiskShipments: number;
    recentActivity: Shipment[];
    mapShipments: Shipment[];
    lastUpdated: Date;
}

export function useDashboardStats() {
    const { isAuthenticated } = useAuth();

    return useQuery({
        queryKey: dashboardQueryKeys.stats(),
        queryFn: async (): Promise<DashboardStats> => {
            const result = await visibilityClient.getDashboardStats();

            if (!result.success) {
                throw new Error("Failed to fetch dashboard stats");
            }

            const response = result.data;
            const shipments = response.content;

            const activeShipments = response.totalElements;

            // Calculate stats from the page of recent shipments
            const delayedCount = shipments.filter(s => (s.delayProbability || 0) > 0.5).length;
            const highRiskCount = shipments.filter(s => (s.delayProbability || 0) > 0.8).length;

            const onTimeCount = activeShipments - delayedCount;
            const onTimePercentage = activeShipments > 0
                ? Math.round((onTimeCount / activeShipments) * 100)
                : 100;

            return {
                activeShipments,
                delayedShipments: delayedCount,
                onTimePercentage,
                highRiskShipments: highRiskCount,
                recentActivity: shipments.slice(0, 5),
                mapShipments: shipments,
                lastUpdated: new Date(),
            };
        },
        enabled: isAuthenticated,
        refetchInterval: DASHBOARD_REFRESH_INTERVAL,
        refetchIntervalInBackground: false, // Pause when tab is inactive
    });
}
