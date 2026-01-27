/**
 * Skeleton loader for KPI cards - prevents layout shift during loading
 */
export function KpiCardSkeleton() {
    return (
        <div className="glass-panel p-6 bg-gradient-to-br from-gray-700/20 to-gray-600/20 border-gray-500/30 shadow-lg min-h-[140px] animate-pulse">
            <div className="flex items-start justify-between mb-4">
                <div className="p-3 rounded-xl bg-white/10 w-12 h-12" />
                <div className="w-16 h-5 bg-white/10 rounded" />
            </div>
            <div className="h-9 w-20 bg-white/10 rounded mb-2" />
            <div className="h-4 w-28 bg-white/10 rounded" />
        </div>
    );
}
