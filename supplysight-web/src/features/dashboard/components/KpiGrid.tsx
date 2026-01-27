import { Package, Clock, AlertTriangle } from 'lucide-react';
import { KpiCard } from './KpiCard';
import { KpiCardSkeleton } from './KpiCardSkeleton';
import { DashboardStats } from '../hooks/useDashboardStats';

interface KpiGridProps {
    stats: DashboardStats | undefined;
    isLoading: boolean;
}

/**
 * Grid of KPI cards for the dashboard
 */
export function KpiGrid({ stats, isLoading }: KpiGridProps) {
    if (isLoading) {
        return (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <KpiCardSkeleton />
                <KpiCardSkeleton />
                <KpiCardSkeleton />
                <KpiCardSkeleton />
            </div>
        );
    }

    return (
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
    );
}
