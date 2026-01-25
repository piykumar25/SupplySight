import { useQuery } from '@tanstack/react-query';
import { visibilityClient } from '@/api';
import { useAuth } from '@/store';
import { Shipment } from '@/types/shipment.types';

export interface DashboardStats {
    activeShipments: number;
    delayedShipments: number;
    onTimePercentage: number;
    highRiskShipments: number;
    recentActivity: Shipment[];
    mapShipments: Shipment[];
}

export function useDashboardStats() {
    const { isAuthenticated } = useAuth();

    return useQuery({
        queryKey: ['dashboard', 'stats'],
        queryFn: async (): Promise<DashboardStats> => {
            const result = await visibilityClient.getDashboardStats();

            if (!result.success) {
                throw new Error("Failed to fetch dashboard stats");
            }

            const response = result.data;
            const shipments = response.content;

            const activeShipments = response.totalElements;

            // Calculate stats from the page of recent shipments
            // In a real app, these should come from specific aggregate endpoints
            const delayedCount = shipments.filter(s => (s.delayProbability || 0) > 0.5).length;
            const highRiskCount = shipments.filter(s => (s.delayProbability || 0) > 0.8).length;

            const onTimeCount = activeShipments - delayedCount; // Rough approximation
            const onTimePercentage = activeShipments > 0
                ? Math.round((onTimeCount / activeShipments) * 100) / 10
                : 100;

            return {
                activeShipments,
                delayedShipments: delayedCount, // This is just for the fetched page, ideally should be total
                onTimePercentage,
                highRiskShipments: highRiskCount,
                recentActivity: shipments.slice(0, 5),
                mapShipments: shipments // Pass all fetched for the map
            };
        },
        enabled: isAuthenticated,
        refetchInterval: 30000, // Refresh every 30 seconds
    });
}
