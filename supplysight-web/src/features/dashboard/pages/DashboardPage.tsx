import { Activity, RefreshCw } from 'lucide-react';
import { useAuth } from '@/store';
import { useDashboardStats } from '../hooks/useDashboardStats';
import { MapWidget } from '@/components/ui/MapWidget';
import { KpiGrid, RecentShipments, StatusBreakdown, TrendsPanel } from '../components';

/**
 * Dashboard Page - Enterprise Command Center
 * Shows real-time KPIs, map, trends, and recent activity
 */
export function DashboardPage() {
    const { user, tenantName } = useAuth();
    const { data: stats, isLoading, error, dataUpdatedAt, refetch, isFetching } = useDashboardStats();

    // Format last updated time
    const lastUpdated = dataUpdatedAt
        ? new Date(dataUpdatedAt).toLocaleTimeString()
        : null;

    if (error) {
        return (
            <div className="p-6 text-red-400 bg-red-900/10 border border-red-500/20 rounded-lg">
                <p className="font-medium mb-2">Failed to load dashboard data</p>
                <p className="text-sm text-gray-400 mb-4">{error.message}</p>
                <button
                    onClick={() => refetch()}
                    className="flex items-center gap-2 px-4 py-2 bg-red-500/20 hover:bg-red-500/30 rounded-lg transition-colors"
                >
                    <RefreshCw className="w-4 h-4" />
                    Retry
                </button>
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
                <div className="flex items-center gap-4">
                    {lastUpdated && (
                        <span className="text-xs text-gray-500">
                            Last updated: {lastUpdated}
                        </span>
                    )}
                    <div className="flex items-center gap-2 text-sm text-gray-400">
                        <Activity className={`w-4 h-4 text-green-400 ${isFetching ? 'animate-spin' : 'animate-pulse'}`} />
                        <span>Live (30s)</span>
                    </div>
                </div>
            </div>

            {/* KPI Grid */}
            <KpiGrid stats={stats} isLoading={isLoading} />

            {/* Map Widget */}
            <div className="h-[400px]">
                <h2 className="text-lg font-semibold text-white mb-4">Live Satellite View</h2>
                {isLoading ? (
                    <div className="h-full bg-white/5 rounded-lg animate-pulse" />
                ) : (
                    <MapWidget shipments={stats?.mapShipments || []} className="h-full" />
                )}
            </div>

            {/* Trends Panel */}
            <TrendsPanel />

            {/* Recent Activity & Stats */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <RecentShipments
                    shipments={stats?.recentActivity || []}
                    isLoading={isLoading}
                />
                <StatusBreakdown
                    stats={stats}
                    isLoading={isLoading}
                />
            </div>
        </div>
    );
}
