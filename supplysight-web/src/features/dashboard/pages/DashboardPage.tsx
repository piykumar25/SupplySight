import { Package, TrendingUp, AlertTriangle, Clock, Activity } from 'lucide-react';
import { useAuth } from '@/store';
import { useDashboardStats } from '../hooks/useDashboardStats';
import { MapWidget } from '@/components/ui/MapWidget';

/**
 * Dashboard placeholder KPI card
 */
interface KpiCardProps {
    title: string;
    value: string | number;
    subtitle?: string;
    icon: React.ElementType;
    trend?: 'up' | 'down' | 'neutral';
    trendValue?: string;
    color: 'primary' | 'green' | 'yellow' | 'red';
}

function KpiCard({ title, value, subtitle, icon: Icon, trend, trendValue, color }: KpiCardProps) {
    const colorClasses = {
        primary: 'from-primary/20 to-blue-600/20 border-primary/30 shadow-primary/10',
        green: 'from-green-500/20 to-emerald-600/20 border-green-500/30 shadow-green-500/10',
        yellow: 'from-yellow-500/20 to-amber-600/20 border-yellow-500/30 shadow-yellow-500/10',
        red: 'from-red-500/20 to-rose-600/20 border-red-500/30 shadow-red-500/10',
    };

    const iconColorClasses = {
        primary: 'text-primary',
        green: 'text-green-400',
        yellow: 'text-yellow-400',
        red: 'text-red-400',
    };

    return (
        <div className={`glass-panel p-6 bg-gradient-to-br ${colorClasses[color]} shadow-lg`}>
            <div className="flex items-start justify-between mb-4">
                <div className={`p-3 rounded-xl bg-white/10 ${iconColorClasses[color]}`}>
                    <Icon className="w-6 h-6" />
                </div>
                {trend && trendValue && (
                    <div className={`flex items-center gap-1 text-sm ${trend === 'up' ? 'text-green-400' : trend === 'down' ? 'text-red-400' : 'text-gray-400'
                        }`}>
                        <TrendingUp className={`w-4 h-4 ${trend === 'down' ? 'rotate-180' : ''}`} />
                        <span>{trendValue}</span>
                    </div>
                )}
            </div>
            <p className="text-3xl font-bold text-white mb-1">{value}</p>
            <p className="text-sm text-gray-400">{title}</p>
            {subtitle && <p className="text-xs text-gray-500 mt-1">{subtitle}</p>}
        </div>
    );
}

/**
 * Dashboard Page
 * Shows KPIs and overview - placeholder for Phase 2 implementation
 */
export function DashboardPage() {
    const { user, tenantName } = useAuth();
    const { data: stats, isLoading, error } = useDashboardStats();

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="p-6 text-red-400 bg-red-900/10 border border-red-500/20 rounded-lg">
                <p>Failed to load dashboard data. Please try again later.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-white">
                        Welcome back, {user?.firstName || 'User'}
                    </h1>
                    <p className="text-gray-400">
                        Here's what's happening with {tenantName || 'your'} shipments today
                    </p>
                </div>
                <div className="flex items-center gap-2 text-sm text-gray-400">
                    <Activity className="w-4 h-4 text-green-400 animate-pulse" />
                    <span>Live updates enabled (30s)</span>
                </div>
            </div>

            {/* KPI Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <KpiCard
                    title="Active Shipments"
                    value={stats?.activeShipments || 0}
                    subtitle="In transit"
                    icon={Package}
                    color="primary"
                    trend="neutral"
                    trendValue="--"
                />
                <KpiCard
                    title="On Time"
                    value={`${stats?.onTimePercentage || 100}%`}
                    subtitle="Predicted on time"
                    icon={Clock}
                    color="green"
                    trend={stats?.onTimePercentage && stats.onTimePercentage > 95 ? 'up' : 'neutral'}
                    trendValue={stats?.onTimePercentage && stats.onTimePercentage > 95 ? 'Great' : 'Normal'}
                />
                <KpiCard
                    title="Delayed"
                    value={stats?.delayedShipments || 0}
                    subtitle="High risk (>50%)"
                    icon={AlertTriangle}
                    color="yellow"
                    trend={stats?.delayedShipments && stats.delayedShipments > 0 ? 'down' : 'neutral'}
                    trendValue={stats?.delayedShipments && stats.delayedShipments > 0 ? 'Action needed' : 'All good'}
                />
                <KpiCard
                    title="High Risk"
                    value={stats?.highRiskShipments || 0}
                    subtitle="Critical (>80%)"
                    icon={AlertTriangle}
                    color="red"
                    trend={stats?.highRiskShipments && stats.highRiskShipments > 0 ? 'down' : 'neutral'}
                    trendValue={stats?.highRiskShipments && stats.highRiskShipments > 0 ? 'Critical' : 'Stable'}
                />
            </div>

            {/* Map Widget */}
            <div className="h-[400px]">
                <h2 className="text-lg font-semibold text-white mb-4">Live Satellite View</h2>
                <MapWidget shipments={stats?.mapShipments || []} className="h-full" />
            </div>

            {/* Recent Activity & Stats */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Recent Shipments List */}
                <div className="lg:col-span-2 glass-panel p-6">
                    <div className="flex items-center justify-between mb-6">
                        <h2 className="text-lg font-semibold text-white">Recent Shipments</h2>
                        <span className="text-sm text-primary cursor-pointer hover:underline">View all</span>
                    </div>
                    {stats?.recentActivity && stats.recentActivity.length > 0 ? (
                        <div className="space-y-4">
                            {stats.recentActivity.map((shipment) => (
                                <div key={shipment.shipmentId} className="flex items-center justify-between p-3 rounded-lg bg-white/5 hover:bg-white/10 transition-colors border border-white/5">
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
                                            ETA: {new Date(shipment.eta || shipment.expectedDeliveryDate).toLocaleDateString()}
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

                {/* Quick Stats Summary */}
                <div className="glass-panel p-6">
                    <h2 className="text-lg font-semibold text-white mb-6">Status Breakdown</h2>
                    <div className="space-y-4">
                        {[
                            { label: 'Active', value: stats?.activeShipments || 0, color: 'bg-primary' },
                            { label: 'Delayed', value: stats?.delayedShipments || 0, color: 'bg-yellow-500' },
                            { label: 'High Risk', value: stats?.highRiskShipments || 0, color: 'bg-red-500' },
                        ].map((stat) => (
                            <div key={stat.label} className="flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                    <div className={`w-2 h-2 rounded-full ${stat.color}`} />
                                    <span className="text-sm text-gray-400">{stat.label}</span>
                                </div>
                                <span className="text-sm font-medium text-white">{stat.value}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}
