import { useParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { RefreshCw } from 'lucide-react';
import {
    Header,
    CurrentStateCard,
    Timeline,
    PredictionPanel,
    MapPanel,
    useShipmentDetailData,
} from '@/features/shipment-detail';

/**
 * Shipment Detail Page
 * Operational workspace for shipment diagnosis and decision-making
 */
export function ShipmentDetailPage() {
    const { id } = useParams<{ id: string }>();
    const [prevUpdatedAt, setPrevUpdatedAt] = useState<string | null>(null);
    const [dataUpdated, setDataUpdated] = useState(false);

    const {
        shipment,
        timeline,
        prediction,
        isLoading,
        isError,
        error,
        isFetching,
        refetchAll,
    } = useShipmentDetailData(id);

    // Detect data updates for pulse animation
    useEffect(() => {
        if (shipment.data?.updatedAt && shipment.data.updatedAt !== prevUpdatedAt) {
            if (prevUpdatedAt !== null) {
                setDataUpdated(true);
                setTimeout(() => setDataUpdated(false), 1000);
            }
            setPrevUpdatedAt(shipment.data.updatedAt);
        }
    }, [shipment.data?.updatedAt, prevUpdatedAt]);

    // Error state
    if (isError) {
        return (
            <div className="space-y-6">
                <Header
                    shipment={undefined}
                    isLoading={false}
                    isFetching={false}
                    onRefresh={refetchAll}
                />
                <div className="glass-panel p-8 text-center">
                    <div className="text-red-400 mb-4">
                        <p className="text-lg font-medium">Failed to load shipment</p>
                        <p className="text-sm text-gray-400 mt-1">
                            {error instanceof Error ? error.message : 'An unexpected error occurred'}
                        </p>
                    </div>
                    <button
                        onClick={refetchAll}
                        className="btn-primary flex items-center gap-2 mx-auto"
                    >
                        <RefreshCw className="w-4 h-4" />
                        Retry
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header with breadcrumb, ID, status, risk */}
            <Header
                shipment={shipment.data}
                isLoading={isLoading}
                isFetching={isFetching}
                onRefresh={refetchAll}
            />

            {/* Top row - three panels */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Current State */}
                <CurrentStateCard
                    shipment={shipment.data}
                    isLoading={shipment.isLoading}
                    dataUpdated={dataUpdated}
                />

                {/* Predictions */}
                <PredictionPanel
                    prediction={prediction.data}
                    isLoading={prediction.isLoading}
                />

                {/* Map */}
                <MapPanel
                    shipment={shipment.data}
                    events={timeline.data?.events || []}
                    isLoading={timeline.isLoading}
                />
            </div>

            {/* Timeline - full width */}
            <Timeline
                events={timeline.data?.events || []}
                isLoading={timeline.isLoading}
            />
        </div>
    );
}
