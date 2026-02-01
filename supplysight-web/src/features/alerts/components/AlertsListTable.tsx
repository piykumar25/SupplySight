/**
 * Alerts List Table Component
 * 
 * Main table for displaying alerts with:
 * - Sortable columns
 * - Row selection for bulk actions
 * - Severity badges
 * - Status indicators
 * - Clickable shipment links
 */

import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
    AlertTriangle,
    CheckCircle2,
    Clock,
    ExternalLink,
    ChevronUp,
    ChevronDown,
    Package
} from 'lucide-react';
import { ROUTES } from '@/lib/constants';
import type { AlertSummary, AlertSeverity, AlertSortColumn, SortDirection } from '../types';
import { getAlertStatus } from '../types';

/**
 * Props for AlertsListTable
 */
interface AlertsListTableProps {
    alerts: AlertSummary[];
    isLoading: boolean;
    sortColumn: AlertSortColumn;
    sortDirection: SortDirection;
    onSort: (column: AlertSortColumn) => void;
    selectedIds: Set<string>;
    onSelectionChange: (ids: Set<string>) => void;
    onAlertClick: (alertId: string) => void;
}

/**
 * Severity badge styling
 */
const severityStyles: Record<AlertSeverity, { bg: string; text: string; border: string }> = {
    CRITICAL: { bg: 'bg-red-500/20', text: 'text-red-400', border: 'border-red-500/30' },
    WARNING: { bg: 'bg-yellow-500/20', text: 'text-yellow-400', border: 'border-yellow-500/30' },
    INFO: { bg: 'bg-blue-500/20', text: 'text-blue-400', border: 'border-blue-500/30' },
};

/**
 * Format date to readable string
 */
function formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
}

/**
 * Format relative time
 */
function formatRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;

    return formatDate(dateString);
}

/**
 * Sort Header Component
 */
function SortHeader({
    column,
    label,
    currentColumn,
    currentDirection,
    onSort,
}: {
    column: AlertSortColumn;
    label: string;
    currentColumn: AlertSortColumn;
    currentDirection: SortDirection;
    onSort: (column: AlertSortColumn) => void;
}) {
    const isActive = column === currentColumn;

    return (
        <button
            onClick={() => onSort(column)}
            className={`flex items-center gap-1 text-xs font-medium uppercase tracking-wider transition-colors ${isActive ? 'text-primary' : 'text-gray-400 hover:text-white'
                }`}
        >
            {label}
            <span className="flex flex-col">
                <ChevronUp
                    className={`w-3 h-3 -mb-1 ${isActive && currentDirection === 'asc' ? 'text-primary' : 'text-gray-600'
                        }`}
                />
                <ChevronDown
                    className={`w-3 h-3 ${isActive && currentDirection === 'desc' ? 'text-primary' : 'text-gray-600'
                        }`}
                />
            </span>
        </button>
    );
}

/**
 * Skeleton Row for loading state
 */
function SkeletonRow() {
    return (
        <tr className="border-b border-white/5">
            <td className="px-4 py-3">
                <div className="w-4 h-4 bg-white/10 rounded animate-pulse" />
            </td>
            <td className="px-4 py-3">
                <div className="w-16 h-5 bg-white/10 rounded animate-pulse" />
            </td>
            <td className="px-4 py-3">
                <div className="w-48 h-5 bg-white/10 rounded animate-pulse" />
            </td>
            <td className="px-4 py-3">
                <div className="w-24 h-5 bg-white/10 rounded animate-pulse" />
            </td>
            <td className="px-4 py-3">
                <div className="w-20 h-5 bg-white/10 rounded animate-pulse" />
            </td>
            <td className="px-4 py-3">
                <div className="w-16 h-5 bg-white/10 rounded animate-pulse" />
            </td>
        </tr>
    );
}

/**
 * Alerts List Table
 */
export function AlertsListTable({
    alerts,
    isLoading,
    sortColumn,
    sortDirection,
    onSort,
    selectedIds,
    onSelectionChange,
    onAlertClick,
}: AlertsListTableProps) {
    // Select all toggle
    const allSelected = alerts.length > 0 && alerts.every((a) => selectedIds.has(a.id));
    const someSelected = alerts.some((a) => selectedIds.has(a.id));

    const handleSelectAll = useCallback(() => {
        if (allSelected) {
            onSelectionChange(new Set());
        } else {
            onSelectionChange(new Set(alerts.map((a) => a.id)));
        }
    }, [alerts, allSelected, onSelectionChange]);

    const handleSelectOne = useCallback(
        (alertId: string) => {
            const newSet = new Set(selectedIds);
            if (newSet.has(alertId)) {
                newSet.delete(alertId);
            } else {
                newSet.add(alertId);
            }
            onSelectionChange(newSet);
        },
        [selectedIds, onSelectionChange]
    );

    return (
        <div className="overflow-x-auto">
            <table className="w-full">
                <thead>
                    <tr className="border-b border-white/10">
                        {/* Checkbox column */}
                        <th className="px-4 py-3 text-left w-12">
                            <input
                                type="checkbox"
                                checked={allSelected}
                                ref={(el) => {
                                    if (el) el.indeterminate = someSelected && !allSelected;
                                }}
                                onChange={handleSelectAll}
                                className="w-4 h-4 rounded border-white/20 bg-white/5 text-primary focus:ring-primary/50 focus:ring-offset-0"
                            />
                        </th>
                        {/* Severity */}
                        <th className="px-4 py-3 text-left">
                            <SortHeader
                                column="severity"
                                label="Severity"
                                currentColumn={sortColumn}
                                currentDirection={sortDirection}
                                onSort={onSort}
                            />
                        </th>
                        {/* Message/Title */}
                        <th className="px-4 py-3 text-left">
                            <span className="text-xs font-medium uppercase tracking-wider text-gray-400">
                                Alert
                            </span>
                        </th>
                        {/* Shipment */}
                        <th className="px-4 py-3 text-left">
                            <span className="text-xs font-medium uppercase tracking-wider text-gray-400">
                                Shipment
                            </span>
                        </th>
                        {/* Created */}
                        <th className="px-4 py-3 text-left">
                            <SortHeader
                                column="createdAt"
                                label="Created"
                                currentColumn={sortColumn}
                                currentDirection={sortDirection}
                                onSort={onSort}
                            />
                        </th>
                        {/* Status */}
                        <th className="px-4 py-3 text-left">
                            <SortHeader
                                column="status"
                                label="Status"
                                currentColumn={sortColumn}
                                currentDirection={sortDirection}
                                onSort={onSort}
                            />
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {isLoading ? (
                        <>
                            <SkeletonRow />
                            <SkeletonRow />
                            <SkeletonRow />
                            <SkeletonRow />
                            <SkeletonRow />
                        </>
                    ) : alerts.length === 0 ? (
                        <tr>
                            <td colSpan={6} className="px-4 py-12 text-center">
                                <AlertTriangle className="w-12 h-12 text-gray-600 mx-auto mb-3" />
                                <p className="text-gray-400">No alerts found</p>
                                <p className="text-sm text-gray-500 mt-1">
                                    Try adjusting your filters
                                </p>
                            </td>
                        </tr>
                    ) : (
                        alerts.map((alert) => {
                            const isSelected = selectedIds.has(alert.id);
                            const severity = severityStyles[alert.severity];
                            const status = getAlertStatus(alert);

                            return (
                                <tr
                                    key={alert.id}
                                    className={`border-b border-white/5 hover:bg-white/5 transition-colors cursor-pointer ${isSelected ? 'bg-primary/10' : ''
                                        }`}
                                    onClick={() => onAlertClick(alert.id)}
                                >
                                    {/* Checkbox */}
                                    <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                                        <input
                                            type="checkbox"
                                            checked={isSelected}
                                            onChange={() => handleSelectOne(alert.id)}
                                            className="w-4 h-4 rounded border-white/20 bg-white/5 text-primary focus:ring-primary/50 focus:ring-offset-0"
                                        />
                                    </td>

                                    {/* Severity */}
                                    <td className="px-4 py-3">
                                        <span
                                            className={`inline-flex items-center gap-1.5 px-2 py-1 rounded text-xs font-medium ${severity.bg} ${severity.text} border ${severity.border}`}
                                        >
                                            <AlertTriangle className="w-3 h-3" />
                                            {alert.severity}
                                        </span>
                                    </td>

                                    {/* Alert Message */}
                                    <td className="px-4 py-3">
                                        <div className="max-w-md">
                                            <p className="text-sm text-white font-medium truncate">
                                                {alert.alertType.replace(/_/g, ' ')}
                                            </p>
                                            <p className="text-xs text-gray-400 truncate mt-0.5">
                                                {alert.message}
                                            </p>
                                        </div>
                                    </td>

                                    {/* Shipment Link */}
                                    <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                                        <Link
                                            to={`${ROUTES.SHIPMENTS}/${alert.shipmentId}`}
                                            className="inline-flex items-center gap-1.5 text-sm text-primary hover:text-primary/80 transition-colors"
                                        >
                                            <Package className="w-3.5 h-3.5" />
                                            <span className="font-mono text-xs">
                                                {alert.shipmentId.substring(0, 8)}...
                                            </span>
                                            <ExternalLink className="w-3 h-3" />
                                        </Link>
                                    </td>

                                    {/* Created */}
                                    <td className="px-4 py-3">
                                        <div className="flex items-center gap-1.5 text-sm text-gray-400">
                                            <Clock className="w-3.5 h-3.5" />
                                            <span>{formatRelativeTime(alert.createdAt)}</span>
                                        </div>
                                    </td>

                                    {/* Status */}
                                    <td className="px-4 py-3">
                                        {status === 'OPEN' ? (
                                            <span className="inline-flex items-center gap-1 text-xs text-yellow-400">
                                                <span className="w-2 h-2 rounded-full bg-yellow-400 animate-pulse" />
                                                Open
                                            </span>
                                        ) : status === 'ACKNOWLEDGED' ? (
                                            <span className="inline-flex items-center gap-1 text-xs text-blue-400">
                                                <CheckCircle2 className="w-3.5 h-3.5" />
                                                Ack'd
                                            </span>
                                        ) : (
                                            <span className="inline-flex items-center gap-1 text-xs text-green-400">
                                                <CheckCircle2 className="w-3.5 h-3.5" />
                                                Resolved
                                            </span>
                                        )}
                                    </td>
                                </tr>
                            );
                        })
                    )}
                </tbody>
            </table>
        </div>
    );
}
