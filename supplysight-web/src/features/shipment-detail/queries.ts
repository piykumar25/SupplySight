import { useQuery } from '@tanstack/react-query';
import { visibilityClient, predictionClient } from '@/api';
import { useAuth } from '@/store';
import { ShipmentDetail, ShipmentEvent, PredictionResult } from '@/types/shipment.types';

/**
 * Shared constants for refresh strategy
 */
const REFETCH_INTERVAL = 30 * 1000; // 30 seconds
const STALE_TIME = 15 * 1000; // 15 seconds

/**
 * Query key factory for shipment detail
 */
export const shipmentDetailKeys = {
    all: ['shipment-detail'] as const,
    detail: (id: string) => [...shipmentDetailKeys.all, 'detail', id] as const,
    timeline: (id: string) => [...shipmentDetailKeys.all, 'timeline', id] as const,
    prediction: (id: string) => [...shipmentDetailKeys.all, 'prediction', id] as const,
};

/**
 * Hook to fetch shipment current state
 */
export function useShipmentDetail(shipmentId: string | undefined) {
    const { isAuthenticated } = useAuth();

    return useQuery<ShipmentDetail>({
        queryKey: shipmentDetailKeys.detail(shipmentId || ''),
        queryFn: async () => {
            if (!shipmentId) throw new Error('No shipment ID');
            const result = await visibilityClient.getShipmentById(shipmentId);
            if (!result.success) {
                throw new Error('Failed to fetch shipment');
            }
            return result.data as ShipmentDetail;
        },
        enabled: isAuthenticated && !!shipmentId,
        refetchInterval: REFETCH_INTERVAL,
        refetchOnWindowFocus: true,
        staleTime: STALE_TIME,
    });
}

/**
 * Hook to fetch shipment timeline
 */
export function useShipmentTimeline(shipmentId: string | undefined) {
    const { isAuthenticated } = useAuth();

    return useQuery<{ shipmentId: string; events: ShipmentEvent[] }>({
        queryKey: shipmentDetailKeys.timeline(shipmentId || ''),
        queryFn: async () => {
            if (!shipmentId) throw new Error('No shipment ID');
            const result = await visibilityClient.getShipmentTimeline(shipmentId);
            if (!result.success) {
                throw new Error('Failed to fetch timeline');
            }
            return result.data;
        },
        enabled: isAuthenticated && !!shipmentId,
        refetchInterval: REFETCH_INTERVAL,
        refetchOnWindowFocus: true,
        staleTime: STALE_TIME,
    });
}

/**
 * Hook to fetch shipment prediction
 */
export function useShipmentPrediction(shipmentId: string | undefined) {
    const { isAuthenticated } = useAuth();

    return useQuery<PredictionResult>({
        queryKey: shipmentDetailKeys.prediction(shipmentId || ''),
        queryFn: async () => {
            if (!shipmentId) throw new Error('No shipment ID');
            const result = await predictionClient.getPrediction(shipmentId);
            if (!result.success) {
                throw new Error('Failed to fetch prediction');
            }
            return result.data;
        },
        enabled: isAuthenticated && !!shipmentId,
        refetchInterval: REFETCH_INTERVAL,
        refetchOnWindowFocus: true,
        staleTime: STALE_TIME,
    });
}

/**
 * Combined hook for all shipment detail data
 */
export function useShipmentDetailData(shipmentId: string | undefined) {
    const detailQuery = useShipmentDetail(shipmentId);
    const timelineQuery = useShipmentTimeline(shipmentId);
    const predictionQuery = useShipmentPrediction(shipmentId);

    return {
        shipment: detailQuery,
        timeline: timelineQuery,
        prediction: predictionQuery,
        isLoading: detailQuery.isLoading || timelineQuery.isLoading,
        isError: detailQuery.isError || timelineQuery.isError,
        error: detailQuery.error || timelineQuery.error,
        isFetching: detailQuery.isFetching || timelineQuery.isFetching || predictionQuery.isFetching,
        refetchAll: () => {
            detailQuery.refetch();
            timelineQuery.refetch();
            predictionQuery.refetch();
        },
    };
}
