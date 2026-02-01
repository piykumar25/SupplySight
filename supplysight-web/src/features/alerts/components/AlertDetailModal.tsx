/**
 * Alert Detail Modal
 * 
 * Modal displaying full alert details with:
 * - Alert header with severity and status
 * - Full message and details
 * - Shipment context
 * - Timeline preview
 * - Action buttons (Acknowledge, Resolve)
 * - Notes section
 */

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
    X,
    AlertTriangle,
    Package,
    Clock,
    CheckCircle2,
    MessageSquare,
    ExternalLink,
    MapPin,
    Truck,
    Loader2,
    Send,
} from 'lucide-react';
import { useAuth } from '@/store';
import { useAlertDetailQuery, useAcknowledgeAlertMutation, useResolveAlertMutation } from '../queries';
import { ROUTES } from '@/lib/constants';
import { getAlertStatus } from '../types';
import type { AlertSeverity, AlertStatus } from '../types';

/**
 * Props for AlertDetailModal
 */
interface AlertDetailModalProps {
    alertId: string | null;
    isOpen: boolean;
    onClose: () => void;
}

/**
 * Severity styling
 */
const severityStyles: Record<AlertSeverity, { bg: string; text: string; border: string; icon: string }> = {
    CRITICAL: { bg: 'bg-red-500/20', text: 'text-red-400', border: 'border-red-500/30', icon: 'text-red-400' },
    WARNING: { bg: 'bg-yellow-500/20', text: 'text-yellow-400', border: 'border-yellow-500/30', icon: 'text-yellow-400' },
    INFO: { bg: 'bg-blue-500/20', text: 'text-blue-400', border: 'border-blue-500/30', icon: 'text-blue-400' },
};

/**
 * Status styling
 */
const statusStyles: Record<AlertStatus, { bg: string; text: string }> = {
    OPEN: { bg: 'bg-yellow-500/20', text: 'text-yellow-400' },
    ACKNOWLEDGED: { bg: 'bg-blue-500/20', text: 'text-blue-400' },
    RESOLVED: { bg: 'bg-green-500/20', text: 'text-green-400' },
};

/**
 * Format date
 */
function formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
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
    if (diffMins < 60) return `${diffMins} minutes ago`;
    if (diffHours < 24) return `${diffHours} hours ago`;
    if (diffDays < 7) return `${diffDays} days ago`;

    return formatDate(dateString);
}

/**
 * Detail Row Component
 */
function DetailRow({ label, value, icon: Icon }: { label: string; value: React.ReactNode; icon?: React.ElementType }) {
    return (
        <div className="flex items-start gap-3 py-3 border-b border-white/5 last:border-b-0">
            {Icon && (
                <div className="p-1.5 rounded-lg bg-white/5 mt-0.5">
                    <Icon className="w-4 h-4 text-gray-400" />
                </div>
            )}
            <div className="flex-1 min-w-0">
                <p className="text-xs text-gray-500 uppercase tracking-wider mb-0.5">{label}</p>
                <div className="text-sm text-white">{value}</div>
            </div>
        </div>
    );
}

/**
 * Alert Detail Modal Component
 */
export function AlertDetailModal({ alertId, isOpen, onClose }: AlertDetailModalProps) {
    const { hasAnyRole } = useAuth();
    const [comment, setComment] = useState('');

    // Queries and mutations
    const alertQuery = useAlertDetailQuery(alertId);
    const acknowledgeMutation = useAcknowledgeAlertMutation();
    const resolveMutation = useResolveAlertMutation();

    // Permissions
    const canAcknowledge = hasAnyRole(['ADMIN', 'OPS_USER']);
    const canResolve = hasAnyRole(['ADMIN', 'OPS_USER']);

    // Reset state when modal closes
    useEffect(() => {
        if (!isOpen) {
            setComment('');
        }
    }, [isOpen]);

    // Close on escape
    useEffect(() => {
        function handleEscape(event: KeyboardEvent) {
            if (event.key === 'Escape') {
                onClose();
            }
        }

        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
            document.body.style.overflow = 'hidden';
        }

        return () => {
            document.removeEventListener('keydown', handleEscape);
            document.body.style.overflow = '';
        };
    }, [isOpen, onClose]);

    if (!isOpen) {
        return null;
    }

    const alert = alertQuery.data;
    const status = alert ? getAlertStatus(alert) : 'OPEN';
    const severity = alert?.severity ?? 'INFO';
    const severityStyle = severityStyles[severity];
    const statusStyle = statusStyles[status];

    const handleAcknowledge = async () => {
        if (!alertId) return;
        try {
            await acknowledgeMutation.mutateAsync({ alertId, comment: comment || undefined });
            setComment('');
        } catch (error) {
            console.error('Failed to acknowledge alert:', error);
        }
    };

    const handleResolve = async () => {
        if (!alertId) return;
        try {
            await resolveMutation.mutateAsync({ alertId, comment: comment || undefined });
            setComment('');
            onClose();
        } catch (error) {
            console.error('Failed to resolve alert:', error);
        }
    };

    const isProcessing = acknowledgeMutation.isPending || resolveMutation.isPending;

    return (
        <>
            {/* Backdrop */}
            <div
                className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50"
                onClick={onClose}
            />

            {/* Modal */}
            <div className="fixed inset-4 lg:inset-y-8 lg:inset-x-auto lg:left-1/2 lg:-translate-x-1/2 lg:w-full lg:max-w-2xl bg-surface border border-white/10 rounded-2xl shadow-2xl z-50 flex flex-col overflow-hidden">
                {/* Header */}
                <div className={`flex items-start justify-between gap-4 p-6 border-b border-white/10 ${severityStyle.bg}`}>
                    <div className="flex items-start gap-4">
                        <div className={`p-3 rounded-xl ${severityStyle.bg} border ${severityStyle.border}`}>
                            <AlertTriangle className={`w-6 h-6 ${severityStyle.icon}`} />
                        </div>
                        <div>
                            <div className="flex items-center gap-2 mb-1">
                                <span className={`text-xs px-2 py-0.5 rounded ${severityStyle.bg} ${severityStyle.text} border ${severityStyle.border}`}>
                                    {severity}
                                </span>
                                <span className={`text-xs px-2 py-0.5 rounded ${statusStyle.bg} ${statusStyle.text}`}>
                                    {status}
                                </span>
                            </div>
                            <h2 className="text-lg font-semibold text-white">
                                {alert?.alertType.replace(/_/g, ' ') ?? 'Loading...'}
                            </h2>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 rounded-lg hover:bg-white/10 transition-colors text-gray-400 hover:text-white"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6">
                    {alertQuery.isLoading ? (
                        <div className="flex items-center justify-center py-12">
                            <Loader2 className="w-8 h-8 text-primary animate-spin" />
                        </div>
                    ) : alertQuery.isError ? (
                        <div className="text-center py-12">
                            <AlertTriangle className="w-12 h-12 text-red-400 mx-auto mb-3" />
                            <p className="text-gray-400">Failed to load alert details</p>
                            <button
                                onClick={() => alertQuery.refetch()}
                                className="mt-3 px-4 py-2 bg-white/10 hover:bg-white/20 rounded-lg text-sm transition-colors"
                            >
                                Retry
                            </button>
                        </div>
                    ) : alert ? (
                        <div className="space-y-6">
                            {/* Message */}
                            <div className="bg-white/5 rounded-xl p-4 border border-white/10">
                                <p className="text-sm text-gray-400 mb-2">Message</p>
                                <p className="text-white">{alert.message}</p>
                            </div>

                            {/* Details Grid */}
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                {/* Left Column */}
                                <div className="bg-white/5 rounded-xl p-4 border border-white/10">
                                    <h3 className="text-sm font-medium text-gray-400 mb-3">Alert Details</h3>

                                    <DetailRow
                                        label="Alert ID"
                                        value={
                                            <span className="font-mono text-xs">{alert.id}</span>
                                        }
                                    />

                                    <DetailRow
                                        label="Created"
                                        value={formatRelativeTime(alert.createdAt)}
                                        icon={Clock}
                                    />

                                    {alert.acknowledged && alert.acknowledgedAt && (
                                        <DetailRow
                                            label="Acknowledged"
                                            value={formatRelativeTime(alert.acknowledgedAt)}
                                            icon={CheckCircle2}
                                        />
                                    )}

                                    {alert.predictionId && (
                                        <DetailRow
                                            label="Prediction ID"
                                            value={
                                                <span className="font-mono text-xs">{alert.predictionId}</span>
                                            }
                                        />
                                    )}
                                </div>

                                {/* Right Column */}
                                <div className="bg-white/5 rounded-xl p-4 border border-white/10">
                                    <h3 className="text-sm font-medium text-gray-400 mb-3">Shipment Context</h3>

                                    <DetailRow
                                        label="Shipment"
                                        value={
                                            <Link
                                                to={`${ROUTES.SHIPMENTS}/${alert.shipmentId}`}
                                                className="inline-flex items-center gap-1.5 text-primary hover:text-primary/80 transition-colors"
                                                onClick={onClose}
                                            >
                                                <span className="font-mono text-xs">{alert.shipmentId.substring(0, 12)}...</span>
                                                <ExternalLink className="w-3 h-3" />
                                            </Link>
                                        }
                                        icon={Package}
                                    />

                                    {alert.details?.location && (
                                        <DetailRow
                                            label="Location"
                                            value={`${alert.details.location.lat?.toFixed(4)}, ${alert.details.location.lng?.toFixed(4)}`}
                                            icon={MapPin}
                                        />
                                    )}

                                    {alert.details?.speed !== undefined && (
                                        <DetailRow
                                            label="Speed"
                                            value={`${alert.details.speed} km/h`}
                                            icon={Truck}
                                        />
                                    )}
                                </div>
                            </div>

                            {/* Additional Details */}
                            {alert.details && Object.keys(alert.details).length > 0 && (
                                <div className="bg-white/5 rounded-xl p-4 border border-white/10">
                                    <h3 className="text-sm font-medium text-gray-400 mb-3">Additional Details</h3>
                                    <pre className="text-xs text-gray-300 bg-black/20 rounded-lg p-3 overflow-x-auto">
                                        {JSON.stringify(alert.details, null, 2)}
                                    </pre>
                                </div>
                            )}

                            {/* Comment Input */}
                            {(canAcknowledge || canResolve) && status !== 'RESOLVED' && (
                                <div className="bg-white/5 rounded-xl p-4 border border-white/10">
                                    <h3 className="text-sm font-medium text-gray-400 mb-3 flex items-center gap-2">
                                        <MessageSquare className="w-4 h-4" />
                                        Add Comment
                                    </h3>
                                    <textarea
                                        value={comment}
                                        onChange={(e) => setComment(e.target.value)}
                                        placeholder="Add a comment (optional)..."
                                        className="w-full px-3 py-2 rounded-lg border border-white/10 bg-white/5 text-sm text-white placeholder-gray-500 focus:border-primary/50 focus:ring-0 focus:outline-none resize-none"
                                        rows={3}
                                    />
                                </div>
                            )}
                        </div>
                    ) : null}
                </div>

                {/* Footer Actions */}
                {alert && (
                    <div className="flex items-center justify-between gap-4 p-6 border-t border-white/10 bg-surface/80">
                        <div className="text-xs text-gray-500">
                            {alert.tenantId && <span>Tenant: {alert.tenantId.substring(0, 8)}...</span>}
                        </div>

                        <div className="flex items-center gap-3">
                            {/* Acknowledge Button */}
                            {canAcknowledge && status === 'OPEN' && (
                                <button
                                    onClick={handleAcknowledge}
                                    disabled={isProcessing}
                                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 disabled:opacity-50 transition-colors text-sm font-medium"
                                >
                                    {acknowledgeMutation.isPending ? (
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                    ) : (
                                        <CheckCircle2 className="w-4 h-4" />
                                    )}
                                    <span>Acknowledge</span>
                                </button>
                            )}

                            {/* Resolve Button */}
                            {canResolve && status !== 'RESOLVED' && (
                                <button
                                    onClick={handleResolve}
                                    disabled={isProcessing}
                                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-green-500/20 text-green-400 hover:bg-green-500/30 disabled:opacity-50 transition-colors text-sm font-medium"
                                >
                                    {resolveMutation.isPending ? (
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                    ) : (
                                        <Send className="w-4 h-4" />
                                    )}
                                    <span>Resolve</span>
                                </button>
                            )}

                            {/* Close Button */}
                            <button
                                onClick={onClose}
                                className="px-4 py-2 rounded-lg border border-white/10 text-gray-400 hover:text-white hover:border-white/20 transition-colors text-sm"
                            >
                                Close
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </>
    );
}
