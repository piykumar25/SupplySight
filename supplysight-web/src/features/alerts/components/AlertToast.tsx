/**
 * Alert Toast Component
 * 
 * Toast notifications for high severity alerts.
 * Renders stacked toasts with auto-dismiss.
 */

import { useEffect, useState } from 'react';
import { AlertTriangle, X, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useToastQueue, useAlertsStore } from '../store';
import { ROUTES } from '@/lib/constants';
import type { AlertSeverity } from '../types';
import type { AlertToast } from '../store';

/**
 * Toast auto-dismiss duration in ms
 */
const TOAST_DURATION = 5000;

/**
 * Severity colors for toasts
 */
const severityStyles: Record<AlertSeverity, { bg: string; border: string; icon: string }> = {
    CRITICAL: {
        bg: 'bg-red-500/10',
        border: 'border-red-500/50',
        icon: 'text-red-400'
    },
    WARNING: {
        bg: 'bg-yellow-500/10',
        border: 'border-yellow-500/50',
        icon: 'text-yellow-400'
    },
    INFO: {
        bg: 'bg-blue-500/10',
        border: 'border-blue-500/50',
        icon: 'text-blue-400'
    },
};

/**
 * Single Toast Item
 */
function ToastItem({ toast, onDismiss }: { toast: AlertToast; onDismiss: () => void }) {
    const [isExiting, setIsExiting] = useState(false);
    const styles = severityStyles[toast.alert.severity];

    // Auto-dismiss timer
    useEffect(() => {
        const timer = setTimeout(() => {
            setIsExiting(true);
            setTimeout(onDismiss, 300); // Wait for exit animation
        }, TOAST_DURATION);

        return () => clearTimeout(timer);
    }, [onDismiss]);

    const handleDismiss = () => {
        setIsExiting(true);
        setTimeout(onDismiss, 300);
    };

    return (
        <div
            className={`
                max-w-sm w-full bg-surface/95 backdrop-blur-xl rounded-xl border shadow-2xl shadow-black/50 overflow-hidden
                transform transition-all duration-300 ease-out
                ${styles.border} ${styles.bg}
                ${isExiting ? 'opacity-0 translate-x-full' : 'opacity-100 translate-x-0'}
            `}
            role="alert"
            aria-live="assertive"
        >
            <div className="p-4">
                <div className="flex items-start gap-3">
                    {/* Icon */}
                    <div className={`p-2 rounded-lg ${styles.bg} flex-shrink-0`}>
                        <AlertTriangle className={`w-5 h-5 ${styles.icon}`} />
                    </div>

                    {/* Content */}
                    <div className="flex-1 min-w-0 pt-0.5">
                        <div className="flex items-center gap-2 mb-1">
                            <span className={`text-xs font-medium ${styles.icon}`}>
                                {toast.alert.alertType.replace(/_/g, ' ')}
                            </span>
                            <span className="text-xs text-gray-500">
                                {toast.alert.severity}
                            </span>
                        </div>
                        <p className="text-sm text-white line-clamp-2">{toast.alert.message}</p>
                    </div>

                    {/* Close button */}
                    <button
                        onClick={handleDismiss}
                        className="p-1 rounded-lg hover:bg-white/10 transition-colors text-gray-400 hover:text-white flex-shrink-0"
                        aria-label="Dismiss notification"
                    >
                        <X className="w-4 h-4" />
                    </button>
                </div>

                {/* Action link */}
                <div className="mt-3 flex justify-end">
                    <Link
                        to={`${ROUTES.ALERTS}?alertId=${toast.alert.id}`}
                        onClick={handleDismiss}
                        className="inline-flex items-center gap-1 text-xs text-primary hover:text-primary/80 transition-colors"
                    >
                        <span>View Details</span>
                        <ExternalLink className="w-3 h-3" />
                    </Link>
                </div>
            </div>

            {/* Progress bar (auto-dismiss indicator) */}
            <div className="h-1 bg-white/5">
                <div
                    className={`h-full ${styles.icon.replace('text-', 'bg-')} origin-left`}
                    style={{
                        animation: `shrink ${TOAST_DURATION}ms linear forwards`,
                    }}
                />
            </div>

            <style>{`
                @keyframes shrink {
                    from { transform: scaleX(1); }
                    to { transform: scaleX(0); }
                }
            `}</style>
        </div>
    );
}

/**
 * Toast Container
 * Renders all active toasts in a stack
 */
export function AlertToastContainer() {
    const toastQueue = useToastQueue();
    const dismissToast = useAlertsStore((state) => state.dismissToast);

    if (toastQueue.length === 0) {
        return null;
    }

    return (
        <div
            className="fixed bottom-4 right-4 z-50 flex flex-col gap-3"
            aria-label="Notifications"
        >
            {toastQueue.map((toast) => (
                <ToastItem
                    key={toast.id}
                    toast={toast}
                    onDismiss={() => dismissToast(toast.id)}
                />
            ))}
        </div>
    );
}
