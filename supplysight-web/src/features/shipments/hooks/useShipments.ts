import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { visibilityClient } from '@/api';
import { useAuth } from '@/store';
import { PaginationParams, PaginatedResponse } from '@/types/api.types';
import { Shipment } from '@/types/shipment.types';

export function useShipments(params: PaginationParams & { status?: string } = {}) {
    const { isAuthenticated } = useAuth();

    // Default params
    const queryParams = {
        page: 0,
        size: 10,
        sortDir: 'desc' as const,
        sortBy: 'updatedAt',
        ...params
    };

    return useQuery<PaginatedResponse<Shipment>>({
        queryKey: ['shipments', queryParams],
        queryFn: async () => {
            const result = await visibilityClient.getShipments(queryParams);
            if (!result.success) {
                throw new Error("Failed to fetch shipments");
            }
            return result.data;
        },
        enabled: isAuthenticated,
        placeholderData: keepPreviousData,
    });
}
