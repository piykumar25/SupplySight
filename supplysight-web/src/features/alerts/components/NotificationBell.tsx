/**
 * Notification Bell Component
 * 
 * Bell icon with unread count badge and dropdown showing latest alerts.
 * Integrates with alert stream for real-time updates.
 */

import { useState, useRef, useEffect } from 'react';
import { Bell, AlertTriangle, Clock, ExternalLink, Wifi, WifiOff } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useUnreadCount, useRecentAlerts } from '../store';
import { useAlertStream } from '../hooks/useAlertStream';
import { ROUTES } from '@/lib/constants';
import type { AlertSummary, AlertSeverity } from '../types';

/**
 * Severity badge colors
 */
const severityColors: Record<AlertSeverity, { bg: string; text: string; border: string }> = {
    CRITICAL: { bg: 'bg-red-500/20', text: 'text-red-400', border: 'border-red-500/30' },
    WARNING: { bg: 'bg-yellow-500/20', text: 'text-yellow-400', border: 'border-yellow-500/30' },
    INFO: { bg: 'bg-blue-500/20', text: 'text-blue-400', border: 'border-blue-500/30' },
};

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

    return date.toLocaleDateString();
}

/**
 * Alert Item in Dropdown
 */
function AlertItem({ alert }: { alert: AlertSummary }) {
    const colors = severityColors[alert.severity];

    return (
        <Link
            to={`${ROUTES.ALERTS}?alertId=${alert.id}`}
            className="block px-4 py-3 hover:bg-white/5 transition-colors border-b border-white/5 last:border-b-0"
        >
            <div className="flex items-start gap-3">
                <div className={`p-1.5 rounded-lg ${colors.bg} flex-shrink-0 mt-0.5`}>
                    <AlertTriangle className={`w-3.5 h-3.5 ${colors.text}`} />
                </div>
                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-0.5">
                        <span className={`text-xs px-1.5 py-0.5 rounded ${colors.bg} ${colors.text} border ${colors.border}`}>
                            {alert.severity}
                        </span>
                        {alert.acknowledged && (
                            <span className="text-xs text-gray-500">Acknowledged</span>
                        )}
                    </div>
                    <p className="text-sm text-white truncate">{alert.message}</p>
                    <div className="flex items-center gap-1 mt-1 text-xs text-gray-500">
                        <Clock className="w-3 h-3" />
                        <span>{formatRelativeTime(alert.createdAt)}</span>
                    </div>
                </div>
            </div>
        </Link>
    );
}

/**
 * Connection Status Indicator
 */
function ConnectionIndicator({ status }: { status: string }) {
    const isConnected = status === 'connected';

    return (
        <div className="flex items-center gap-1.5 px-4 py-2 border-b border-white/10 bg-surface/50">
            {isConnected ? (
                <>
                    <Wifi className="w-3.5 h-3.5 text-green-400" />
                    <span className="text-xs text-green-400">Live</span>
                </>
            ) : status === 'connecting' || status === 'reconnecting' ? (
                <>
                    <Wifi className="w-3.5 h-3.5 text-yellow-400 animate-pulse" />
                    <span className="text-xs text-yellow-400">Connecting...</span>
                </>
            ) : (
                <>
                    <WifiOff className="w-3.5 h-3.5 text-gray-500" />
                    <span className="text-xs text-gray-500">Offline</span>
                </>
            )}
        </div>
    );
}

/**
 * Notification Bell with Dropdown
 */
export function NotificationBell() {
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    // Connect to alert stream
    const { connectionStatus } = useAlertStream({ debug: true });

    // Get data from store
    const unreadCount = useUnreadCount();
    const recentAlerts = useRecentAlerts();

    // Close on outside click
    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        }

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen]);

    // Close on escape
    useEffect(() => {
        function handleEscape(event: KeyboardEvent) {
            if (event.key === 'Escape') {
                setIsOpen(false);
            }
        }

        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
        }

        return () => {
            document.removeEventListener('keydown', handleEscape);
        };
    }, [isOpen]);

    return (
        <div className="relative" ref={dropdownRef}>
            {/* Bell Button */}
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="relative p-2 rounded-lg hover:bg-white/10 transition-colors text-gray-400 hover:text-white"
                aria-label={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`}
            >
                <Bell className="w-5 h-5" />

                {/* Unread Badge */}
                {unreadCount > 0 && (
                    <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] flex items-center justify-center px-1 text-[10px] font-bold bg-red-500 text-white rounded-full shadow-lg shadow-red-500/30">
                        {unreadCount > 99 ? '99+' : unreadCount}
                    </span>
                )}

                {/* Connection indicator dot */}
                <span
                    className={`absolute bottom-0.5 right-0.5 w-2 h-2 rounded-full border border-surface ${connectionStatus === 'connected'
                        ? 'bg-green-400'
                        : connectionStatus === 'connecting' || connectionStatus === 'reconnecting'
                            ? 'bg-yellow-400 animate-pulse'
                            : 'bg-gray-500'
                        }`}
                />
            </button>

            {/* Dropdown */}
            {isOpen && (
                <div className="absolute right-0 mt-2 w-80 bg-surface/95 backdrop-blur-xl border border-white/10 rounded-xl shadow-2xl shadow-black/50 overflow-hidden z-50">
                    {/* Header */}
                    <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
                        <h3 className="text-sm font-semibold text-white">Notifications</h3>
                        {unreadCount > 0 && (
                            <span className="text-xs text-gray-400">{unreadCount} unread</span>
                        )}
                    </div>

                    {/* Connection Status */}
                    <ConnectionIndicator status={connectionStatus} />

                    {/* Alerts List */}
                    <div className="max-h-[360px] overflow-y-auto">
                        {recentAlerts.length > 0 ? (
                            recentAlerts.map((alert) => (
                                <AlertItem key={alert.id} alert={alert} />
                            ))
                        ) : (
                            <div className="py-8 text-center">
                                <Bell className="w-8 h-8 text-gray-600 mx-auto mb-2" />
                                <p className="text-sm text-gray-400">No recent alerts</p>
                            </div>
                        )}
                    </div>

                    {/* Footer */}
                    <Link
                        to={ROUTES.ALERTS}
                        onClick={() => setIsOpen(false)}
                        className="flex items-center justify-center gap-2 px-4 py-3 border-t border-white/10 text-sm text-primary hover:bg-white/5 transition-colors"
                    >
                        <span>View all alerts</span>
                        <ExternalLink className="w-3.5 h-3.5" />
                    </Link>
                </div>
            )}
        </div>
    );
}
