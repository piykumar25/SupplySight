import { useParams } from 'react-router-dom';
import { Package, MapPin, Clock, ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/lib/constants';

/**
 * Shipment Detail Page
 * Placeholder for Phase 4 implementation
 */
export function ShipmentDetailPage() {
    const { id } = useParams<{ id: string }>();

    return (
        <div className="space-y-6">
            {/* Back navigation */}
            <Link
                to={ROUTES.SHIPMENTS}
                className="inline-flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
            >
                <ArrowLeft className="w-4 h-4" />
                <span>Back to Shipments</span>
            </Link>

            {/* Header */}
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-white">Shipment Details</h1>
                    <p className="text-gray-400 font-mono">ID: {id || 'Unknown'}</p>
                </div>
            </div>

            {/* Placeholder panels */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Current State */}
                <div className="glass-panel p-6">
                    <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                        <Package className="w-5 h-5 text-primary" />
                        Current State
                    </h2>
                    <div className="flex flex-col items-center justify-center h-32 text-gray-500">
                        <p className="text-sm text-center">
                            Status, location, ETA, and risk score will be displayed here.
                        </p>
                    </div>
                </div>

                {/* Location */}
                <div className="glass-panel p-6">
                    <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                        <MapPin className="w-5 h-5 text-primary" />
                        Last Location
                    </h2>
                    <div className="flex flex-col items-center justify-center h-32 text-gray-500">
                        <p className="text-sm text-center">
                            Map visualization and coordinates will be shown here.
                        </p>
                    </div>
                </div>

                {/* Predictions */}
                <div className="glass-panel p-6">
                    <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                        <Clock className="w-5 h-5 text-primary" />
                        Predictions
                    </h2>
                    <div className="flex flex-col items-center justify-center h-32 text-gray-500">
                        <p className="text-sm text-center">
                            ETA, delay probability, and anomaly flags will appear here.
                        </p>
                    </div>
                </div>
            </div>

            {/* Timeline placeholder */}
            <div className="glass-panel p-6">
                <h2 className="text-lg font-semibold text-white mb-4">Event Timeline</h2>
                <div className="flex flex-col items-center justify-center h-48 text-gray-500">
                    <p className="text-sm text-center max-w-md">
                        Chronological event history with out-of-order event correction will be implemented in Phase 4.
                    </p>
                    <p className="text-sm text-primary mt-4">Phase 4 Implementation</p>
                </div>
            </div>
        </div>
    );
}
