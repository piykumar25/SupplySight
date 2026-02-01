/**
 * useAlertStream Hook
 * React hook for managing the alert stream connection
 * 
 * Features:
 * - Auto-connect on mount with tenant context
 * - Auto-disconnect on unmount
 * - Syncs stream events to Zustand store
 * - Exposes connection status
 */

import { useEffect, useCallback, useRef } from 'react';
import { useAuth } from '@/store';
import { getAlertStreamManager } from '../stream';
import { useAlertsStore } from '../store';
import type { ConnectionStatus } from '../types';

interface UseAlertStreamOptions {
    /** Auto-connect when hook mounts (default: true) */
    autoConnect?: boolean;
    /** Enable console logging of events (default: false) */
    debug?: boolean;
}

interface UseAlertStreamReturn {
    /** Current connection status */
    connectionStatus: ConnectionStatus;
    /** Whether using polling fallback */
    isPolling: boolean;
    /** Manually connect to stream */
    connect: () => void;
    /** Manually disconnect from stream */
    disconnect: () => void;
    /** Unread alert count */
    unreadCount: number;
}

/**
 * Hook for managing alert stream connection
 */
export function useAlertStream(options: UseAlertStreamOptions = {}): UseAlertStreamReturn {
    const { autoConnect = true, debug = false } = options;

    // Auth context
    const { tenantId, isAuthenticated } = useAuth();
    const accessTokenRef = useRef<() => string | null>(() => null);

    // Store state
    const connectionStatus = useAlertsStore((state) => state.connectionStatus);
    const isPolling = useAlertsStore((state) => state.isPolling);
    const unreadCount = useAlertsStore((state) => state.unreadCount);
    const setConnectionStatus = useAlertsStore((state) => state.setConnectionStatus);
    const setIsPolling = useAlertsStore((state) => state.setIsPolling);
    const processStreamEvent = useAlertsStore((state) => state.processStreamEvent);

    // Get fresh access token from auth store
    // We import useAuthStore directly to get the latest token
    useEffect(() => {
        import('@/store').then(({ useAuthStore }) => {
            accessTokenRef.current = () => useAuthStore.getState().accessToken;
        });
    }, []);

    // Connect to stream
    const connect = useCallback(() => {
        if (!tenantId || !isAuthenticated) {
            if (debug) {
                console.log('[useAlertStream] Cannot connect: not authenticated');
            }
            return;
        }

        const manager = getAlertStreamManager();

        if (debug) {
            console.log('[useAlertStream] Connecting to alert stream...');
        }

        manager.connect({
            tenantId,
            getAccessToken: accessTokenRef.current,
        });
    }, [tenantId, isAuthenticated, debug]);

    // Disconnect from stream
    const disconnect = useCallback(() => {
        const manager = getAlertStreamManager();
        manager.disconnect();

        if (debug) {
            console.log('[useAlertStream] Disconnected from alert stream');
        }
    }, [debug]);

    // Set up event subscriptions
    useEffect(() => {
        const manager = getAlertStreamManager();

        // Subscribe to connection status changes
        const unsubConnection = manager.onConnectionChange((status) => {
            setConnectionStatus(status);
            setIsPolling(manager.isPolling());

            if (debug) {
                console.log('[useAlertStream] Connection status:', status);
            }
        });

        // Subscribe to alert events
        const unsubEvents = manager.onAlertEvent((event) => {
            if (debug) {
                console.log('[useAlertStream] Alert event:', event.type, event.data);
            }
            processStreamEvent(event);
        });

        // Subscribe to errors
        const unsubErrors = manager.onError((error) => {
            if (debug) {
                console.error('[useAlertStream] Stream error:', error);
            }
        });

        return () => {
            unsubConnection();
            unsubEvents();
            unsubErrors();
        };
    }, [setConnectionStatus, setIsPolling, processStreamEvent, debug]);

    // Auto-connect on mount
    useEffect(() => {
        if (autoConnect && isAuthenticated && tenantId) {
            connect();
        }

        return () => {
            // Don't disconnect on unmount - other components may be using the stream
            // The stream is a singleton and will persist
        };
    }, [autoConnect, isAuthenticated, tenantId, connect]);

    return {
        connectionStatus,
        isPolling,
        connect,
        disconnect,
        unreadCount,
    };
}

/**
 * Demo consumer hook that logs all events (for testing)
 */
export function useAlertStreamDemo(): void {
    useAlertStream({ debug: true });
}
