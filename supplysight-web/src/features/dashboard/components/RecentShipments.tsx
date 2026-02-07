import { Package } from 'lucide-react';
import { Shipment } from '@/types/shipment.types';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/lib/constants';

interface RecentShipmentsProps {
    shipments: Shipment[];
    isLoading?: boolean;
}

/**
 * Skeleton loader for recent shipments list
 */
function RecentShipmentsSkeleton() {
    return (
        <div className="space-y-4">
            {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="flex items-center justify-between p-3 rounded-lg bg-white/5 animate-pulse">
                    <div className="flex items-center gap-4">
                        <div className="p-2 rounded-full bg-primary/20 w-9 h-9" />
                        <div>
                            <div className="h-4 w-24 bg-white/10 rounded mb-2" />
                            <div className="h-3 w-32 bg-white/10 rounded" />
                        </div>
                    </div>
                    <div className="text-right">
                        <div className="h-6 w-16 bg-white/10 rounded mb-2" />
                        <div className="h-3 w-20 bg-white/10 rounded" />
                    </div>
                </div>
            ))}
        </div>
    );
}

/**
 * Recent shipments list component
 */
export function RecentShipments({ shipments, isLoading }: RecentShipmentsProps) {
    if (isLoading) {
        return (
            <div className="lg:col-span-2 glass-panel p-6">
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-lg font-semibold text-white">Recent Shipments</h2>
                    <span className="text-sm text-primary cursor-pointer hover:underline">View all</span>
                </div>
                <RecentShipmentsSkeleton />
            </div>
        );
    }

    return (
        <div className="lg:col-span-2 glass-panel p-6">
            <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-semibold text-white">Recent Shipments</h2>
                <Link to={ROUTES.SHIPMENTS} className="text-sm text-primary cursor-pointer hover:underline">
                    View all
                </Link>
            </div>
            {shipments.length > 0 ? (
                <div className="space-y-4">
                    {shipments.map((shipment) => (
                        <div
                            key={shipment.shipmentId}
                            className="flex items-center justify-between p-3 rounded-lg bg-white/5 hover:bg-white/10 transition-colors border border-white/5"
                        >
                            <div className="flex items-center gap-4">
                                <div className="p-2 rounded-full bg-primary/20 text-primary">
                                    <Package className="w-5 h-5" />
                                </div>
                                <div>
                                    <p className="font-medium text-white">{shipment.trackingNumber}</p>
                                    <p className="text-sm text-gray-400">
                                        {shipment.origin?.hubCode || 'Origin'} → {shipment.destination?.hubCode || 'Dest'}
                                    </p>
                                </div>
                            </div>
                            <div className="text-right">
                                <div className={`text-sm px-2 py-1 rounded-full inline-block ${shipment.status === 'DELIVERED' ? 'bg-green-500/20 text-green-400' :
                                    shipment.status === 'DELAYED' ? 'bg-red-500/20 text-red-400' :
                                        'bg-blue-500/20 text-blue-400'
                                    }`}>
                                    {shipment.status}
                                </div>
                                <p className="text-xs text-gray-500 mt-1">
                                    ETA: {(shipment.eta || shipment.expectedDeliveryDate) ? new Date((shipment.eta || shipment.expectedDeliveryDate)!).toLocaleDateString() : 'N/A'}
                                </p>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                <div className="flex flex-col items-center justify-center h-48 text-gray-500">
                    <Package className="w-12 h-12 mb-4 opacity-50" />
                    <p className="text-sm">No shipments found</p>
                </div>
            )}
        </div>
    );
}
