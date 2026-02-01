/**
 * Alerts Store
 * Zustand store for managing alert state including:
 * - Unread count for bell icon
 * - Recent alerts for dropdown
 * - Toast queue for high severity alerts
 * - Connection status tracking
 */

import { create } from 'zustand';
import type { AlertSummary, AlertStreamEvent, ConnectionStatus } from './types';
import { isHighSeverityAlert } from './types';

/**
 * Toast notification for display
 */
export interface AlertToast {
    id: string;
    alert: AlertSummary;
    timestamp: number;
}

/**
 * Alerts store state
 */
interface AlertsState {
    // Connection
    connectionStatus: ConnectionStatus;
    isPolling: boolean;

    // Counts
    unreadCount: number;
    totalCount: number;

    // Recent alerts for dropdown (max 10)
    recentAlerts: AlertSummary[];

    // Toast queue for high severity (max 3 visible)
    toastQueue: AlertToast[];

    // Notification preferences
    browserNotificationsEnabled: boolean;
    toastNotificationsEnabled: boolean;
}

/**
 * Alerts store actions
 */
interface AlertsActions {
    // Connection management
    setConnectionStatus: (status: ConnectionStatus) => void;
    setIsPolling: (isPolling: boolean) => void;

    // Count management
    setUnreadCount: (count: number) => void;
    setTotalCount: (count: number) => void;
    decrementUnreadCount: () => void;

    // Alert management
    addRecentAlert: (alert: AlertSummary) => void;
    updateRecentAlert: (alertId: string, updates: Partial<AlertSummary>) => void;
    removeRecentAlert: (alertId: string) => void;
    setRecentAlerts: (alerts: AlertSummary[]) => void;

    // Toast management
    addToast: (alert: AlertSummary) => void;
    dismissToast: (toastId: string) => void;
    clearAllToasts: () => void;

    // Notification preferences
    setBrowserNotificationsEnabled: (enabled: boolean) => void;
    setToastNotificationsEnabled: (enabled: boolean) => void;

    // Process incoming stream event
    processStreamEvent: (event: AlertStreamEvent) => void;

    // Reset store
    reset: () => void;
}

type AlertsStore = AlertsState & AlertsActions;

/**
 * Initial state
 */
const initialState: AlertsState = {
    connectionStatus: 'disconnected',
    isPolling: false,
    unreadCount: 0,
    totalCount: 0,
    recentAlerts: [],
    toastQueue: [],
    browserNotificationsEnabled: false,
    toastNotificationsEnabled: true,
};

/**
 * Max recent alerts to keep
 */
const MAX_RECENT_ALERTS = 10;

/**
 * Max visible toasts
 */
const MAX_VISIBLE_TOASTS = 3;

/**
 * Create the alerts store
 */
export const useAlertsStore = create<AlertsStore>((set, get) => ({
    ...initialState,

    // Connection management
    setConnectionStatus: (status) => set({ connectionStatus: status }),
    setIsPolling: (isPolling) => set({ isPolling }),

    // Count management
    setUnreadCount: (count) => set({ unreadCount: count }),
    setTotalCount: (count) => set({ totalCount: count }),
    decrementUnreadCount: () => set((state) => ({
        unreadCount: Math.max(0, state.unreadCount - 1)
    })),

    // Alert management
    addRecentAlert: (alert) => set((state) => {
        // Check if alert already exists
        const exists = state.recentAlerts.some((a) => a.id === alert.id);
        if (exists) {
            return state;
        }

        // Add to front, limit to max
        const newAlerts = [alert, ...state.recentAlerts].slice(0, MAX_RECENT_ALERTS);
        return {
            recentAlerts: newAlerts,
            unreadCount: state.unreadCount + 1,
        };
    }),

    updateRecentAlert: (alertId, updates) => set((state) => ({
        recentAlerts: state.recentAlerts.map((a) =>
            a.id === alertId ? { ...a, ...updates } : a
        ),
    })),

    removeRecentAlert: (alertId) => set((state) => ({
        recentAlerts: state.recentAlerts.filter((a) => a.id !== alertId),
    })),

    setRecentAlerts: (alerts) => set({
        recentAlerts: alerts.slice(0, MAX_RECENT_ALERTS)
    }),

    // Toast management
    addToast: (alert) => set((state) => {
        // Check if toast for this alert already exists
        const exists = state.toastQueue.some((t) => t.alert.id === alert.id);
        if (exists) {
            return state;
        }

        const toast: AlertToast = {
            id: `toast-${alert.id}-${Date.now()}`,
            alert,
            timestamp: Date.now(),
        };

        // Add to queue, limit visible toasts
        const newQueue = [...state.toastQueue, toast].slice(-MAX_VISIBLE_TOASTS);
        return { toastQueue: newQueue };
    }),

    dismissToast: (toastId) => set((state) => ({
        toastQueue: state.toastQueue.filter((t) => t.id !== toastId),
    })),

    clearAllToasts: () => set({ toastQueue: [] }),

    // Notification preferences
    setBrowserNotificationsEnabled: (enabled) => set({ browserNotificationsEnabled: enabled }),
    setToastNotificationsEnabled: (enabled) => set({ toastNotificationsEnabled: enabled }),

    // Process incoming stream event
    processStreamEvent: (event) => {
        const state = get();

        switch (event.type) {
            case 'alert:new': {
                // Add to recent alerts
                get().addRecentAlert(event.data);

                // Show toast for high severity
                if (isHighSeverityAlert(event.data) && state.toastNotificationsEnabled) {
                    get().addToast(event.data);
                }

                // Show browser notification if enabled
                if (isHighSeverityAlert(event.data) && state.browserNotificationsEnabled) {
                    showBrowserNotification(event.data);
                }

                // Log for debugging
                console.log('[AlertsStore] New alert received:', event.data);
                break;
            }

            case 'alert:acknowledged': {
                get().updateRecentAlert(event.data.id, { acknowledged: true });
                get().decrementUnreadCount();
                console.log('[AlertsStore] Alert acknowledged:', event.data.id);
                break;
            }

            case 'alert:resolved': {
                get().updateRecentAlert(event.data.id, { acknowledged: true });
                console.log('[AlertsStore] Alert resolved:', event.data.id);
                break;
            }

            case 'alert:updated': {
                get().updateRecentAlert(event.data.id, event.data);
                console.log('[AlertsStore] Alert updated:', event.data.id);
                break;
            }
        }
    },

    // Reset store
    reset: () => set(initialState),
}));

/**
 * Show browser notification for high severity alerts
 */
function showBrowserNotification(alert: AlertSummary): void {
    if (!('Notification' in window)) {
        return;
    }

    if (Notification.permission !== 'granted') {
        return;
    }

    try {
        const notification = new Notification('SupplySight Alert', {
            body: alert.message,
            icon: '/favicon.ico',
            tag: alert.id, // Prevents duplicate notifications
            requireInteraction: alert.severity === 'CRITICAL',
        });

        notification.onclick = () => {
            window.focus();
            notification.close();
            // Could navigate to alert detail here
        };

        // Auto-close after 10 seconds (unless critical)
        if (alert.severity !== 'CRITICAL') {
            setTimeout(() => notification.close(), 10000);
        }

    } catch (error) {
        console.error('[AlertsStore] Failed to show browser notification:', error);
    }
}

/**
 * Request browser notification permission
 */
export async function requestNotificationPermission(): Promise<boolean> {
    if (!('Notification' in window)) {
        console.warn('[AlertsStore] Browser does not support notifications');
        return false;
    }

    if (Notification.permission === 'granted') {
        return true;
    }

    if (Notification.permission === 'denied') {
        console.warn('[AlertsStore] Notification permission denied');
        return false;
    }

    try {
        const permission = await Notification.requestPermission();
        return permission === 'granted';
    } catch (error) {
        console.error('[AlertsStore] Failed to request notification permission:', error);
        return false;
    }
}

/**
 * Selector hooks for common use cases
 */
export const useUnreadCount = () => useAlertsStore((state) => state.unreadCount);
export const useRecentAlerts = () => useAlertsStore((state) => state.recentAlerts);
export const useToastQueue = () => useAlertsStore((state) => state.toastQueue);
export const useConnectionStatus = () => useAlertsStore((state) => state.connectionStatus);
