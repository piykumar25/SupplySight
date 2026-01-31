import { useNavigate } from 'react-router-dom';
import { AlertTriangle, CheckCircle, Clock } from 'lucide-react';
import { Shipment, ShipmentStatus } from '@/types/shipment.types';
import { SortableColumn, SortDirection, getRiskLevel } from '../types';
import { SortableHeader } from './SortableHeader';
import { RowActions } from './RowActions';

interface ShipmentsTableProps {
    data: Shipment[];
    isLoading: boolean;
    sortColumn: SortableColumn;
    sortDirection: SortDirection;
    onSort: (column: SortableColumn) => void;
}

// Skeleton row for loading state
function SkeletonRow() {
    return (
        <tr className="animate-pulse">
            {[...Array(6)].map((_, i) => (
                <td key={i} className="px-6 py-4">
                    <div className="h-4 bg-white/10 rounded w-3/4" />
                </td>
            ))}
        </tr>
    );
}

// Status badge styles
const statusStyles: Record<ShipmentStatus, string> = {
    CREATED: 'bg-gray-500/20 text-gray-400',
    PICKED_UP: 'bg-blue-500/20 text-blue-400',
    IN_TRANSIT: 'bg-blue-600/20 text-blue-300',
    DELIVERED: 'bg-green-500/20 text-green-400',
    EXCEPTION: 'bg-red-500/20 text-red-400',
    DELAYED: 'bg-yellow-500/20 text-yellow-400',
};

export function ShipmentsTable({
    data,
    isLoading,
    sortColumn,
    sortDirection,
    onSort,
}: ShipmentsTableProps) {
    const navigate = useNavigate();

    // Skeleton loading state
    if (isLoading && data.length === 0) {
        return (
            <div className="overflow-x-auto rounded-lg border border-white/10 bg-black/20">
                <table className="w-full text-left">
                    <thead className="bg-white/5 text-gray-400 text-xs uppercase">
                        <tr>
                            <th className="px-6 py-3 font-medium">Tracking Number</th>
                            <th className="px-6 py-3 font-medium">Route</th>
                            <th className="px-6 py-3 font-medium">Status</th>
                            <th className="px-6 py-3 font-medium">Risk</th>
                            <th className="px-6 py-3 font-medium">ETA</th>
                            <th className="px-6 py-3 font-medium">Last Updated</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                        {[...Array(5)].map((_, i) => (
                            <SkeletonRow key={i} />
                        ))}
                    </tbody>
                </table>
            </div>
        );
    }

    // Empty state
    if (!isLoading && data.length === 0) {
        return (
            <div className="w-full h-64 flex flex-col items-center justify-center bg-black/20 rounded-lg border border-white/5">
                <div className="text-gray-500 text-center">
                    <p className="text-lg font-medium">No shipments found</p>
                    <p className="text-sm mt-1">Try adjusting your filters</p>
                </div>
            </div>
        );
    }

    return (
        <div className={`overflow-x-auto rounded-lg border border-white/10 bg-black/20 ${isLoading ? 'opacity-70' : ''} transition-opacity`}>
            <table className="w-full text-left min-w-[800px]">
                <thead className="bg-white/5 text-gray-400 text-xs uppercase">
                    <tr>
                        <th className="px-6 py-3 font-medium">Tracking Number</th>
                        <th className="px-6 py-3 font-medium">Route</th>
                        <th className="px-6 py-3 font-medium">
                            <SortableHeader
                                column="status"
                                label="Status"
                                currentColumn={sortColumn}
                                currentDirection={sortDirection}
                                onSort={onSort}
                            />
                        </th>
                        <th className="px-6 py-3 font-medium">
                            <SortableHeader
                                column="delayProbability"
                                label="Risk"
                                currentColumn={sortColumn}
                                currentDirection={sortDirection}
                                onSort={onSort}
                            />
                        </th>
                        <th className="px-6 py-3 font-medium">
                            <SortableHeader
                                column="eta"
                                label="ETA"
                                currentColumn={sortColumn}
                                currentDirection={sortDirection}
                                onSort={onSort}
                            />
                        </th>
                        <th className="px-6 py-3 font-medium">
                            <SortableHeader
                                column="updatedAt"
                                label="Last Updated"
                                currentColumn={sortColumn}
                                currentDirection={sortDirection}
                                onSort={onSort}
                            />
                        </th>
                        <th className="px-6 py-3 font-medium w-24">Actions</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                    {data.map((shipment) => {
                        const riskLevel = getRiskLevel(shipment.delayProbability);
                        const isHighRisk = riskLevel === 'high';

                        return (
                            <tr
                                key={shipment.shipmentId}
                                className={`
                                    hover:bg-white/5 transition-colors cursor-pointer group
                                    ${isHighRisk ? 'bg-red-500/5 hover:bg-red-500/10' : ''}
                                `}
                                onClick={() => navigate(`/shipments/${shipment.shipmentId}`)}
                            >
                                <td className="px-6 py-4 text-sm font-medium text-white">
                                    {shipment.trackingNumber}
                                </td>
                                <td className="px-6 py-4 text-sm">
                                    <div className="flex flex-col">
                                        <span className="text-white">{shipment.origin?.hubCode || 'Origin'}</span>
                                        <span className="text-xs text-gray-500">to</span>
                                        <span className="text-white">{shipment.destination?.hubCode || 'Destination'}</span>
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusStyles[shipment.status] || 'bg-gray-500/20 text-gray-400'}`}>
                                        {shipment.status.replace('_', ' ')}
                                    </span>
                                </td>
                                <td className="px-6 py-4 text-sm">
                                    {riskLevel === 'high' && (
                                        <span className="flex items-center gap-1 text-red-400">
                                            <AlertTriangle className="w-4 h-4" /> High
                                        </span>
                                    )}
                                    {riskLevel === 'delayed' && (
                                        <span className="flex items-center gap-1 text-yellow-400">
                                            <Clock className="w-4 h-4" /> Med
                                        </span>
                                    )}
                                    {riskLevel === 'normal' && (
                                        <span className="flex items-center gap-1 text-green-400">
                                            <CheckCircle className="w-4 h-4" /> Low
                                        </span>
                                    )}
                                </td>
                                <td className="px-6 py-4 text-sm">
                                    <div className="flex flex-col">
                                        <span className="text-white">
                                            {new Date(shipment.eta || shipment.expectedDeliveryDate).toLocaleDateString()}
                                        </span>
                                        <span className="text-xs text-gray-500">
                                            {new Date(shipment.eta || shipment.expectedDeliveryDate).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                        </span>
                                    </div>
                                </td>
                                <td className="px-6 py-4 text-sm text-gray-300">
                                    {new Date(shipment.updatedAt).toLocaleString()}
                                </td>
                                <td className="px-6 py-4">
                                    <RowActions shipmentId={shipment.shipmentId} />
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
