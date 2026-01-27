/**
 * Centralized query keys for dashboard feature
 * Ensures cache consistency across components
 */
export const dashboardQueryKeys = {
    all: ['dashboard'] as const,
    stats: () => [...dashboardQueryKeys.all, 'stats'] as const,
    trends: (period: '7d' | '30d') => [...dashboardQueryKeys.all, 'trends', period] as const,
    alerts: () => [...dashboardQueryKeys.all, 'alerts'] as const,
};

/**
 * Refresh interval for dashboard data (30 seconds)
 */
export const DASHBOARD_REFRESH_INTERVAL = 30 * 1000;
