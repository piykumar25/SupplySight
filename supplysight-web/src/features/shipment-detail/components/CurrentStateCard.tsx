import { useEffect, useState } from 'react';
import { Package, MapPin, Clock, AlertTriangle, TrendingUp, Target } from 'lucide-react';
import { ShipmentDetail, ShipmentStatus } from '@/types/shipment.types';
import { STATUS_CONFIG, getRiskLevel, RISK_CONFIG } from '../types';

interface CurrentStateCardProps {
    shipment: ShipmentDetail | undefined;
    isLoading: boolean;
    dataUpdated?: boolean; // Trigger pulse animation
}

function Skeleton() {
    return (
        <div className="glass-panel p-6 animate-pulse">
            <div className="flex items-center gap-2 mb-4">
                <div className="w-5 h-5 bg-white/10 rounded" />
                <div className="h-5 w-32 bg-white/10 rounded" />
            </div>
            <div className="space-y-4">
                {[...Array(5)].map((_, i) => (
                    <div key={i} className="flex justify-between">
                        <div className="h-4 w-24 bg-white/10 rounded" />
                        <div className="h-4 w-32 bg-white/10 rounded" />
                    </div>
                ))}
            </div>
        </div>
    );
}

export function CurrentStateCard({ shipment, isLoading, dataUpdated }: CurrentStateCardProps) {
    const [showPulse, setShowPulse] = useState(false);

    // Trigger pulse on data update
    useEffect(() => {
        if (dataUpdated) {
            setShowPulse(true);
            const timer = setTimeout(() => setShowPulse(false), 1000);
            return () => clearTimeout(timer);
        }
    }, [dataUpdated]);

    if (isLoading) {
        return <Skeleton />;
    }

    if (!shipment) {
        return (
            <div className="glass-panel p-6 text-center text-gray-500">
                No shipment data available
            </div>
        );
    }

    const status = shipment.status as ShipmentStatus;
    const statusConfig = STATUS_CONFIG[status] || STATUS_CONFIG.IN_TRANSIT;
    const riskLevel = getRiskLevel(shipment.delayProbability);
    const riskConfig = RISK_CONFIG[riskLevel];

    return (
        <div className={`glass-panel p-6 transition-all ${showPulse ? 'ring-2 ring-primary/50' : ''}`}>
            <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                <Package className="w-5 h-5 text-primary" />
                Current State
            </h2>

            <div className="space-y-4">
                {/* Status */}
                <div className="flex items-start justify-between">
                    <span className="text-gray-400 text-sm">Status</span>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusConfig.bgColor} ${statusConfig.color}`}>
                        {statusConfig.label}
                    </span>
                </div>

                {/* Last Location */}
                <div className="flex items-start justify-between">
                    <span className="text-gray-400 text-sm flex items-center gap-1">
                        <MapPin className="w-4 h-4" />
                        Location
                    </span>
                    <div className="text-right">
                        <div className="text-white text-sm">
                            {shipment.lastLocation?.hubCode || 'Unknown'}
                        </div>
                        {shipment.lastLocation?.lat && shipment.lastLocation?.lon && (
                            <div className="text-gray-500 text-xs">
                                {shipment.lastLocation.lat.toFixed(4)}, {shipment.lastLocation.lon.toFixed(4)}
                            </div>
                        )}
                    </div>
                </div>

                {/* ETA */}
                <div className="flex items-start justify-between">
                    <span className="text-gray-400 text-sm flex items-center gap-1">
                        <Clock className="w-4 h-4" />
                        ETA
                    </span>
                    <div className="text-right">
                        <div className="text-white text-sm">
                            {shipment.eta
                                ? new Date(shipment.eta).toLocaleDateString()
                                : 'N/A'
                            }
                        </div>
                        {shipment.eta && (
                            <div className="text-gray-500 text-xs">
                                {new Date(shipment.eta).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </div>
                        )}
                    </div>
                </div>

                {/* Delay Probability */}
                <div className="flex items-start justify-between">
                    <span className="text-gray-400 text-sm flex items-center gap-1">
                        <AlertTriangle className="w-4 h-4" />
                        Delay Risk
                    </span>
                    <div className="flex items-center gap-2">
                        <div className="w-24 h-2 bg-white/10 rounded-full overflow-hidden">
                            <div
                                className={`h-full transition-all ${riskLevel === 'high' ? 'bg-red-500' :
                                    riskLevel === 'delayed' ? 'bg-yellow-500' :
                                        'bg-green-500'
                                    }`}
                                style={{ width: `${(shipment.delayProbability || 0) * 100}%` }}
                            />
                        </div>
                        <span className={`text-sm font-medium ${riskConfig.color}`}>
                            {((shipment.delayProbability || 0) * 100).toFixed(0)}%
                        </span>
                    </div>
                </div>

                {/* Route info */}
                <div className="pt-3 border-t border-white/10">
                    <div className="flex items-center justify-between text-sm">
                        <div className="flex items-center gap-2">
                            <Target className="w-4 h-4 text-green-400" />
                            <span className="text-gray-400">Origin</span>
                            <span className="text-white">{shipment.origin?.hubCode || 'N/A'}</span>
                        </div>
                        <TrendingUp className="w-4 h-4 text-gray-500" />
                        <div className="flex items-center gap-2">
                            <span className="text-white">{shipment.destination?.hubCode || 'N/A'}</span>
                            <span className="text-gray-400">Destination</span>
                            <Target className="w-4 h-4 text-primary" />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
