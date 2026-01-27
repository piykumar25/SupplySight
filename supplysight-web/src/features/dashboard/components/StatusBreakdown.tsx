import { DashboardStats } from '../hooks/useDashboardStats';

interface StatusBreakdownProps {
    stats: DashboardStats | undefined;
    isLoading?: boolean;
}

/**
 * Skeleton for status breakdown
 */
function StatusBreakdownSkeleton() {
    return (
        <div className="space-y-4">
            {[1, 2, 3].map((i) => (
                <div key={i} className="flex items-center justify-between animate-pulse">
                    <div className="flex items-center gap-3">
                        <div className="w-2 h-2 rounded-full bg-white/20" />
                        <div className="h-4 w-16 bg-white/10 rounded" />
                    </div>
                    <div className="h-4 w-8 bg-white/10 rounded" />
                </div>
            ))}
        </div>
    );
}

/**
 * Status breakdown panel showing shipment counts by status
 */
export function StatusBreakdown({ stats, isLoading }: StatusBreakdownProps) {
    const breakdownData = [
        { label: 'Active', value: stats?.activeShipments || 0, color: 'bg-primary' },
        { label: 'Delayed', value: stats?.delayedShipments || 0, color: 'bg-yellow-500' },
        { label: 'High Risk', value: stats?.highRiskShipments || 0, color: 'bg-red-500' },
    ];

    return (
        <div className="glass-panel p-6">
            <h2 className="text-lg font-semibold text-white mb-6">Status Breakdown</h2>
            {isLoading ? (
                <StatusBreakdownSkeleton />
            ) : (
                <div className="space-y-4">
                    {breakdownData.map((stat) => (
                        <div key={stat.label} className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                                <div className={`w-2 h-2 rounded-full ${stat.color}`} />
                                <span className="text-sm text-gray-400">{stat.label}</span>
                            </div>
                            <span className="text-sm font-medium text-white">{stat.value}</span>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
