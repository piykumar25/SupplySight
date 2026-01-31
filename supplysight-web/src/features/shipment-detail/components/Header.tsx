import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Copy, Check, RefreshCw, AlertTriangle, Clock, CheckCircle } from 'lucide-react';
import { ROUTES } from '@/lib/constants';
import { ShipmentDetail, ShipmentStatus } from '@/types/shipment.types';
import { getRiskLevel, RISK_CONFIG, STATUS_CONFIG } from '../types';

interface HeaderProps {
    shipment: ShipmentDetail | undefined;
    isLoading: boolean;
    isFetching: boolean;
    onRefresh: () => void;
}

export function Header({ shipment, isLoading, isFetching, onRefresh }: HeaderProps) {
    const [copied, setCopied] = useState(false);

    const handleCopyId = () => {
        if (shipment?.shipmentId) {
            navigator.clipboard.writeText(shipment.shipmentId);
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        }
    };

    const riskLevel = getRiskLevel(shipment?.delayProbability);
    const riskConfig = RISK_CONFIG[riskLevel];
    const status = shipment?.status as ShipmentStatus | undefined;
    const statusConfig = status ? STATUS_CONFIG[status] : null;

    // Risk icon
    const RiskIcon = riskLevel === 'high' ? AlertTriangle : riskLevel === 'delayed' ? Clock : CheckCircle;

    return (
        <div className="space-y-4">
            {/* Breadcrumb */}
            <Link
                to={ROUTES.SHIPMENTS}
                className="inline-flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
            >
                <ArrowLeft className="w-4 h-4" />
                <span>Back to Shipments</span>
            </Link>

            {/* Header row */}
            <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
                {/* Left side - Title and ID */}
                <div className="space-y-2">
                    {isLoading ? (
                        <>
                            <div className="h-8 w-48 bg-white/10 rounded animate-pulse" />
                            <div className="h-5 w-64 bg-white/10 rounded animate-pulse" />
                        </>
                    ) : (
                        <>
                            <div className="flex items-center gap-3">
                                <h1 className="text-2xl font-bold text-white">
                                    Shipment Details
                                </h1>
                                {/* Status badge */}
                                {statusConfig && (
                                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${statusConfig.bgColor} ${statusConfig.color}`}>
                                        {statusConfig.label}
                                    </span>
                                )}
                            </div>

                            {/* Shipment ID (copyable) */}
                            <div className="flex items-center gap-2">
                                <span className="text-gray-400 font-mono text-sm">
                                    ID: {shipment?.shipmentId}
                                </span>
                                <button
                                    onClick={handleCopyId}
                                    className="p-1 rounded hover:bg-white/10 transition-colors"
                                    title="Copy ID"
                                >
                                    {copied ? (
                                        <Check className="w-4 h-4 text-green-400" />
                                    ) : (
                                        <Copy className="w-4 h-4 text-gray-400" />
                                    )}
                                </button>
                            </div>
                        </>
                    )}
                </div>

                {/* Right side - Risk indicator and refresh */}
                <div className="flex items-center gap-4">
                    {/* Risk indicator */}
                    {!isLoading && shipment && (
                        <div className={`flex items-center gap-2 px-3 py-2 rounded-lg ${riskConfig.bgColor}`}>
                            <RiskIcon className={`w-5 h-5 ${riskConfig.color}`} />
                            <span className={`text-sm font-medium ${riskConfig.color}`}>
                                {riskConfig.label}
                            </span>
                        </div>
                    )}

                    {/* Last updated & refresh */}
                    <div className="flex items-center gap-2 text-sm text-gray-400">
                        {shipment?.updatedAt && (
                            <span>
                                Updated {new Date(shipment.updatedAt).toLocaleTimeString()}
                            </span>
                        )}
                        <button
                            onClick={onRefresh}
                            disabled={isFetching}
                            className="p-2 rounded-lg hover:bg-white/10 transition-colors disabled:opacity-50"
                            title="Refresh"
                        >
                            <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
