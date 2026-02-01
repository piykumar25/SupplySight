/**
 * Alerts Page
 * 
 * Full alerts management page with:
 * - Summary cards
 * - Filters and search
 * - Data table with sorting and selection
 * - Bulk actions
 * - Pagination
 */

import { useState, useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AlertTriangle, Bell, CheckCircle2, Clock, RefreshCw, Download } from 'lucide-react';
import { useAuth } from '@/store';
import { useAlertsQuery, useAlertCountQuery } from '../queries';
import { AlertsListTable } from '../components/AlertsListTable';
import { AlertsFilters } from '../components/AlertsFilters';
import { Pagination } from '../components/Pagination';
import { BulkActions } from '../components/BulkActions';
import { AlertDetailModal } from '../components/AlertDetailModal';
import {
    DEFAULT_ALERT_FILTERS,
    DEFAULT_ALERT_SORT,
    DEFAULT_ALERT_PAGINATION,
} from '../types';
import type {
    AlertFilters,
    AlertSortConfig,
    AlertPagination,
    AlertSortColumn,
} from '../types';

/**
 * Alerts Page Component
 */
export function AlertsPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const { hasAnyRole } = useAuth();

    // Get alert ID from URL for modal
    const selectedAlertId = searchParams.get('alertId');

    // State
    const [filters, setFilters] = useState<AlertFilters>(DEFAULT_ALERT_FILTERS);
    const [sort, setSort] = useState<AlertSortConfig>(DEFAULT_ALERT_SORT);
    const [pagination, setPagination] = useState<AlertPagination>(DEFAULT_ALERT_PAGINATION);
    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

    // Query config
    const queryConfig = useMemo(
        () => ({ filters, sort, pagination }),
        [filters, sort, pagination]
    );

    // Queries
    const alertsQuery = useAlertsQuery(queryConfig);
    const countQuery = useAlertCountQuery();

    // Permissions
    const canAcknowledge = hasAnyRole(['ADMIN', 'OPS_USER']);
    const canResolve = hasAnyRole(['ADMIN', 'OPS_USER']);

    // Event handlers
    const handleSort = useCallback((column: AlertSortColumn) => {
        setSort((prev) => ({
            column,
            direction: prev.column === column && prev.direction === 'desc' ? 'asc' : 'desc',
        }));
        // Reset to first page on sort change
        setPagination((prev) => ({ ...prev, page: 0 }));
    }, []);

    const handleFiltersChange = useCallback((newFilters: AlertFilters) => {
        setFilters(newFilters);
        // Reset to first page on filter change
        setPagination((prev) => ({ ...prev, page: 0 }));
        // Clear selection
        setSelectedIds(new Set());
    }, []);

    const handlePageChange = useCallback((page: number) => {
        setPagination((prev) => ({ ...prev, page }));
        setSelectedIds(new Set());
    }, []);

    const handlePageSizeChange = useCallback((size: number) => {
        setPagination((prev) => ({ ...prev, size, page: 0 }));
        setSelectedIds(new Set());
    }, []);

    const handleAlertClick = useCallback((alertId: string) => {
        // Update URL with alert ID to open modal
        setSearchParams({ alertId });
    }, [setSearchParams]);

    const handleModalClose = useCallback(() => {
        // Remove alertId from URL to close modal
        setSearchParams({});
    }, [setSearchParams]);

    const handleRefresh = useCallback(() => {
        alertsQuery.refetch();
        countQuery.refetch();
    }, [alertsQuery, countQuery]);

    const handleExportCSV = useCallback(() => {
        // TODO: Implement CSV export
        console.log('Export CSV clicked');
    }, []);

    // Derived data
    const alerts = alertsQuery.data?.content ?? [];
    const totalItems = alertsQuery.data?.totalElements ?? 0;
    const totalPages = alertsQuery.data?.totalPages ?? 0;

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-white">Alerts</h1>
                    <p className="text-gray-400 text-sm mt-1">
                        Monitor and manage shipment alerts and exceptions
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        onClick={handleRefresh}
                        disabled={alertsQuery.isFetching}
                        className="flex items-center gap-2 px-3 py-2 rounded-lg border border-white/10 bg-white/5 text-gray-400 hover:text-white hover:border-white/20 disabled:opacity-50 transition-colors text-sm"
                    >
                        <RefreshCw className={`w-4 h-4 ${alertsQuery.isFetching ? 'animate-spin' : ''}`} />
                        <span>Refresh</span>
                    </button>
                    <button
                        onClick={handleExportCSV}
                        className="flex items-center gap-2 px-3 py-2 rounded-lg border border-white/10 bg-white/5 text-gray-400 hover:text-white hover:border-white/20 transition-colors text-sm"
                    >
                        <Download className="w-4 h-4" />
                        <span>Export</span>
                    </button>
                </div>
            </div>

            {/* Summary Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {/* Total Alerts */}
                <SummaryCard
                    title="Total Alerts"
                    value={countQuery.data?.total ?? 0}
                    icon={Bell}
                    iconBg="bg-blue-500/20"
                    iconColor="text-blue-400"
                    isLoading={countQuery.isLoading}
                />

                {/* Open Alerts */}
                <SummaryCard
                    title="Open"
                    value={countQuery.data?.unacknowledged ?? 0}
                    icon={AlertTriangle}
                    iconBg="bg-yellow-500/20"
                    iconColor="text-yellow-400"
                    isLoading={countQuery.isLoading}
                    highlight={true}
                />

                {/* Acknowledged */}
                <SummaryCard
                    title="Acknowledged"
                    value={(countQuery.data?.total ?? 0) - (countQuery.data?.unacknowledged ?? 0)}
                    icon={CheckCircle2}
                    iconBg="bg-green-500/20"
                    iconColor="text-green-400"
                    isLoading={countQuery.isLoading}
                />

                {/* Avg Response Time (placeholder) */}
                <SummaryCard
                    title="Avg Response"
                    value="2.4h"
                    icon={Clock}
                    iconBg="bg-purple-500/20"
                    iconColor="text-purple-400"
                    isLoading={false}
                    subtitle="Last 7 days"
                />
            </div>

            {/* Filters */}
            <div className="bg-surface/50 rounded-xl border border-white/10 p-4">
                <AlertsFilters filters={filters} onFiltersChange={handleFiltersChange} />
            </div>

            {/* Bulk Actions */}
            {selectedIds.size > 0 && (
                <BulkActions
                    selectedIds={selectedIds}
                    onClearSelection={() => setSelectedIds(new Set())}
                    canAcknowledge={canAcknowledge}
                    canResolve={canResolve}
                />
            )}

            {/* Table */}
            <div className="bg-surface/50 rounded-xl border border-white/10 overflow-hidden">
                <AlertsListTable
                    alerts={alerts}
                    isLoading={alertsQuery.isLoading}
                    sortColumn={sort.column}
                    sortDirection={sort.direction}
                    onSort={handleSort}
                    selectedIds={selectedIds}
                    onSelectionChange={setSelectedIds}
                    onAlertClick={handleAlertClick}
                />

                {/* Pagination */}
                {totalItems > 0 && (
                    <div className="p-4 border-t border-white/10">
                        <Pagination
                            pagination={pagination}
                            totalItems={totalItems}
                            totalPages={totalPages}
                            onPageChange={handlePageChange}
                            onPageSizeChange={handlePageSizeChange}
                        />
                    </div>
                )}
            </div>

            {/* Error State */}
            {alertsQuery.isError && (
                <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 text-center">
                    <p className="text-red-400">Failed to load alerts. Please try again.</p>
                    <button
                        onClick={handleRefresh}
                        className="mt-2 px-4 py-2 bg-red-500/20 hover:bg-red-500/30 rounded-lg text-red-400 text-sm transition-colors"
                    >
                        Retry
                    </button>
                </div>
            )}

            {/* Alert Detail Modal */}
            <AlertDetailModal
                alertId={selectedAlertId}
                isOpen={!!selectedAlertId}
                onClose={handleModalClose}
            />
        </div>
    );
}

/**
 * Summary Card Component
 */
function SummaryCard({
    title,
    value,
    icon: Icon,
    iconBg,
    iconColor,
    isLoading,
    highlight = false,
    subtitle,
}: {
    title: string;
    value: number | string;
    icon: React.ElementType;
    iconBg: string;
    iconColor: string;
    isLoading: boolean;
    highlight?: boolean;
    subtitle?: string;
}) {
    return (
        <div
            className={`p-4 rounded-xl border ${highlight
                ? 'bg-yellow-500/5 border-yellow-500/30'
                : 'bg-surface/50 border-white/10'
                }`}
        >
            <div className="flex items-start justify-between">
                <div>
                    <p className="text-xs text-gray-400 uppercase tracking-wider">{title}</p>
                    {isLoading ? (
                        <div className="w-16 h-8 bg-white/10 rounded animate-pulse mt-2" />
                    ) : (
                        <p className={`text-2xl font-bold mt-1 ${highlight ? 'text-yellow-400' : 'text-white'}`}>
                            {value}
                        </p>
                    )}
                    {subtitle && <p className="text-xs text-gray-500 mt-1">{subtitle}</p>}
                </div>
                <div className={`p-2 rounded-lg ${iconBg}`}>
                    <Icon className={`w-5 h-5 ${iconColor}`} />
                </div>
            </div>
        </div>
    );
}
