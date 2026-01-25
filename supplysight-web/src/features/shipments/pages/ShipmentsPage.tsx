import { useState } from 'react';
import { Package, Search, Filter, AlertTriangle, CheckCircle, Clock } from 'lucide-react';
import { useShipments } from '../hooks/useShipments';
import { DataTable, Column } from '@/components/ui/DataTable';
import { Shipment } from '@/types/shipment.types';
import { useNavigate } from 'react-router-dom';

/**
 * Shipments Page
 * Phase 2 Implementation
 */

export function ShipmentsPage() {
    const navigate = useNavigate();
    const [page, setPage] = useState(0);
    const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
    // TODO: Connect setStatusFilter to the UI filter button

    // Pagination params
    const { data, isLoading } = useShipments({
        page,
        size: 10,
        status: statusFilter,
        sortDir: 'desc'
    });

    const columns: Column<Shipment>[] = [
        {
            header: 'Tracking Number',
            accessorKey: 'trackingNumber',
            className: 'font-medium text-white',
        },
        {
            header: 'Route',
            cell: (shipment) => (
                <div className="flex flex-col">
                    <span className="text-white">{shipment.origin?.hubCode || 'Origin'}</span>
                    <span className="text-xs text-gray-500">to</span>
                    <span className="text-white">{shipment.destination?.hubCode || 'Destination'}</span>
                </div>
            )
        },
        {
            header: 'Status',
            cell: (shipment) => {
                const statusStyles = {
                    CREATED: 'bg-gray-500/20 text-gray-400',
                    PICKED_UP: 'bg-blue-500/20 text-blue-400',
                    IN_TRANSIT: 'bg-blue-600/20 text-blue-300',
                    DELIVERED: 'bg-green-500/20 text-green-400',
                    EXCEPTION: 'bg-red-500/20 text-red-400',
                    DELAYED: 'bg-yellow-500/20 text-yellow-400',
                };

                const style = statusStyles[shipment.status] || 'bg-gray-500/20 text-gray-400';

                return (
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${style}`}>
                        {shipment.status.replace('_', ' ')}
                    </span>
                );
            }
        },
        {
            header: 'Risk',
            cell: (shipment) => {
                if ((shipment.delayProbability || 0) > 0.8) {
                    return <span className="flex items-center gap-1 text-red-400"><AlertTriangle className="w-4 h-4" /> High</span>;
                }
                if ((shipment.delayProbability || 0) > 0.5) {
                    return <span className="flex items-center gap-1 text-yellow-400"><Clock className="w-4 h-4" /> Med</span>;
                }
                return <span className="flex items-center gap-1 text-green-400"><CheckCircle className="w-4 h-4" /> Low</span>;
            }
        },
        {
            header: 'ETA',
            cell: (shipment) => (
                <div className="flex flex-col">
                    <span className="text-white">
                        {new Date(shipment.eta || shipment.expectedDeliveryDate).toLocaleDateString()}
                    </span>
                    <span className="text-xs text-gray-500">
                        {new Date(shipment.eta || shipment.expectedDeliveryDate).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                </div>
            )
        },
        {
            header: 'Last Updated',
            cell: (shipment) => new Date(shipment.updatedAt).toLocaleString()
        }
    ];

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-white">Shipments</h1>
                    <p className="text-gray-400">Track and manage all your shipments</p>
                </div>
                <div className="flex items-center gap-3">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                        <input
                            type="text"
                            placeholder="Search shipments..."
                            className="input-field pl-10 w-64"
                            disabled // Backend search implementation pending
                        />
                    </div>
                    {/* Filter buttons could go here */}
                </div>
            </div>

            {/* List */}
            <div className="glass-panel p-6">
                <DataTable
                    data={data?.content || []}
                    columns={columns}
                    isLoading={isLoading}
                    pagination={data ? {
                        page: data.page,
                        totalPages: data.totalPages,
                        totalElements: data.totalElements,
                        onPageChange: setPage
                    } : undefined}
                    onRowClick={(shipment) => navigate(`/shipments/${shipment.shipmentId}`)}
                />
            </div>
        </div>
    );
}
