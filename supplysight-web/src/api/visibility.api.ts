import { visibilityApi } from './axios';
import { PaginatedResponse, ApiResult } from '../types/api.types';
import { Shipment, ShipmentEvent } from '../types/shipment.types';

export const visibilityClient = {
    /**
     * Get a list of shipments with optional filtering
     */
    getShipments: async (params?: { page?: number; size?: number; status?: string }) => {
        const response = await visibilityApi.get<ApiResult<PaginatedResponse<Shipment>>>(
            '/shipments',
            { params }
        );
        return response.data;
    },

    /**
     * Get a single shipment by ID
     */
    getShipmentById: async (id: string) => {
        const response = await visibilityApi.get<ApiResult<Shipment>>(
            `/shipments/${id}`
        );
        return response.data;
    },

    /**
     * Get shipment timeline/events
     */
    getShipmentTimeline: async (id: string) => {
        const response = await visibilityApi.get<ApiResult<{ shipmentId: string; events: ShipmentEvent[] }>>(
            `/shipments/${id}/timeline`
        );
        return response.data;
    },

    /**
     * Get shipment stats
     */
    getDashboardStats: async () => {
        // Fetch last 100 active shipments to calculate quick stats
        const response = await visibilityApi.get<ApiResult<PaginatedResponse<Shipment>>>(
            '/shipments',
            {
                params: {
                    page: 0,
                    size: 100,
                    sortDir: 'desc',
                    sortBy: 'updatedAt'
                }
            }
        );
        return response.data;
    }
};
