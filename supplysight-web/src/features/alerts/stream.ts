/**
 * Alert Stream Manager
 * Handles SSE connection with automatic polling fallback
 * 
 * Features:
 * - Server-Sent Events for real-time updates
 * - Automatic fallback to polling if SSE unavailable
 * - Exponential backoff on connection failures
 * - Shared subscription (multiple consumers, single connection)
 * - Connection state management
 */

import { API_BASE_URLS } from '@/lib/constants';
import type { AlertStreamEvent, AlertSummary, ConnectionStatus } from './types';

// Event callback types
type AlertEventCallback = (event: AlertStreamEvent) => void;
type ConnectionCallback = (status: ConnectionStatus) => void;
type ErrorCallback = (error: Error) => void;

// Stream configuration
interface StreamConfig {
    /** Tenant ID for SSE subscription */
    tenantId: string;
    /** Access token getter function */
    getAccessToken: () => string | null;
    /** Polling interval in ms (fallback) */
    pollingInterval?: number;
    /** Max reconnect attempts before giving up */
    maxReconnectAttempts?: number;
    /** Initial reconnect delay in ms */
    initialReconnectDelay?: number;
    /** Max reconnect delay in ms */
    maxReconnectDelay?: number;
}

// Default configuration values
const DEFAULT_POLLING_INTERVAL = 30000; // 30 seconds
const DEFAULT_MAX_RECONNECT_ATTEMPTS = 10;
const DEFAULT_INITIAL_RECONNECT_DELAY = 1000; // 1 second
const DEFAULT_MAX_RECONNECT_DELAY = 60000; // 1 minute

/**
 * Alert Stream Manager Class
 * Singleton pattern - use getInstance() to get shared instance
 */
class AlertStreamManager {
    private static instance: AlertStreamManager | null = null;

    // Configuration
    private config: StreamConfig | null = null;

    // Connection state
    private eventSource: EventSource | null = null;
    private pollingTimer: ReturnType<typeof setInterval> | null = null;
    private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
    private connectionStatus: ConnectionStatus = 'disconnected';
    private reconnectAttempts = 0;
    private lastEventTimestamp: string | null = null;
    private isUsingFallback = false;

    // Subscribers
    private eventSubscribers: Set<AlertEventCallback> = new Set();
    private connectionSubscribers: Set<ConnectionCallback> = new Set();
    private errorSubscribers: Set<ErrorCallback> = new Set();

    private constructor() {
        // Private constructor for singleton
    }

    /**
     * Get singleton instance
     */
    static getInstance(): AlertStreamManager {
        if (!AlertStreamManager.instance) {
            AlertStreamManager.instance = new AlertStreamManager();
        }
        return AlertStreamManager.instance;
    }

    /**
     * Initialize and connect the stream
     */
    connect(config: StreamConfig): void {
        this.config = {
            ...config,
            pollingInterval: config.pollingInterval ?? DEFAULT_POLLING_INTERVAL,
            maxReconnectAttempts: config.maxReconnectAttempts ?? DEFAULT_MAX_RECONNECT_ATTEMPTS,
            initialReconnectDelay: config.initialReconnectDelay ?? DEFAULT_INITIAL_RECONNECT_DELAY,
            maxReconnectDelay: config.maxReconnectDelay ?? DEFAULT_MAX_RECONNECT_DELAY,
        };

        this.connectSSE();
    }

    /**
     * Disconnect and cleanup
     */
    disconnect(): void {
        this.cleanup();
        this.updateConnectionStatus('disconnected');
        this.reconnectAttempts = 0;
        this.isUsingFallback = false;
    }

    /**
     * Get current connection status
     */
    getConnectionStatus(): ConnectionStatus {
        return this.connectionStatus;
    }

    /**
     * Check if using polling fallback
     */
    isPolling(): boolean {
        return this.isUsingFallback;
    }

    /**
     * Subscribe to alert events
     */
    onAlertEvent(callback: AlertEventCallback): () => void {
        this.eventSubscribers.add(callback);
        return () => this.eventSubscribers.delete(callback);
    }

    /**
     * Subscribe to connection status changes
     */
    onConnectionChange(callback: ConnectionCallback): () => void {
        this.connectionSubscribers.add(callback);
        // Immediately notify of current status
        callback(this.connectionStatus);
        return () => this.connectionSubscribers.delete(callback);
    }

    /**
     * Subscribe to errors
     */
    onError(callback: ErrorCallback): () => void {
        this.errorSubscribers.add(callback);
        return () => this.errorSubscribers.delete(callback);
    }

    // Private methods

    private connectSSE(): void {
        if (!this.config) {
            console.error('[AlertStream] No config provided');
            return;
        }

        // Check if SSE is supported
        if (typeof EventSource === 'undefined') {
            console.warn('[AlertStream] SSE not supported, falling back to polling');
            this.startPolling();
            return;
        }

        this.updateConnectionStatus('connecting');

        const token = this.config.getAccessToken();
        if (!token) {
            console.warn('[AlertStream] No auth token, waiting...');
            this.scheduleReconnect();
            return;
        }

        try {
            // Build SSE URL with tenant ID
            // Note: SSE doesn't support custom headers, so we pass token as query param
            // The backend should validate the token from query param for SSE endpoints
            const sseUrl = new URL(`${API_BASE_URLS.PREDICTION}/alerts/stream`);
            sseUrl.searchParams.set('tenantId', this.config.tenantId);
            sseUrl.searchParams.set('token', token);

            this.eventSource = new EventSource(sseUrl.toString());

            this.eventSource.onopen = () => {
                console.log('[AlertStream] SSE connected');
                this.updateConnectionStatus('connected');
                this.reconnectAttempts = 0;
                this.isUsingFallback = false;
            };

            this.eventSource.onmessage = (event) => {
                this.handleSSEMessage(event);
            };

            // Listen for specific event types
            this.eventSource.addEventListener('alert:new', (event) => {
                this.handleSSEMessage(event, 'alert:new');
            });

            this.eventSource.addEventListener('alert:updated', (event) => {
                this.handleSSEMessage(event, 'alert:updated');
            });

            this.eventSource.addEventListener('alert:acknowledged', (event) => {
                this.handleSSEMessage(event, 'alert:acknowledged');
            });

            this.eventSource.addEventListener('alert:resolved', (event) => {
                this.handleSSEMessage(event, 'alert:resolved');
            });

            this.eventSource.onerror = (error) => {
                console.error('[AlertStream] SSE error:', error);
                this.handleConnectionError(new Error('SSE connection failed'));
            };

        } catch (error) {
            console.error('[AlertStream] Failed to create EventSource:', error);
            this.handleConnectionError(error as Error);
        }
    }

    private handleSSEMessage(event: MessageEvent, type?: string): void {
        try {
            const data = JSON.parse(event.data) as AlertSummary;
            const streamEvent: AlertStreamEvent = {
                type: (type || 'alert:new') as AlertStreamEvent['type'],
                data,
                timestamp: new Date().toISOString(),
            };

            this.lastEventTimestamp = streamEvent.timestamp;
            this.notifyEventSubscribers(streamEvent);

        } catch (error) {
            console.error('[AlertStream] Failed to parse SSE message:', error);
        }
    }

    private handleConnectionError(error: Error): void {
        this.cleanup();
        this.updateConnectionStatus('reconnecting');
        this.notifyErrorSubscribers(error);

        // Check if we should try SSE reconnect or switch to polling
        if (this.reconnectAttempts >= (this.config?.maxReconnectAttempts ?? DEFAULT_MAX_RECONNECT_ATTEMPTS)) {
            console.warn('[AlertStream] Max reconnect attempts reached, switching to polling');
            this.startPolling();
        } else {
            this.scheduleReconnect();
        }
    }

    private scheduleReconnect(): void {
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
        }

        // Exponential backoff
        const delay = Math.min(
            (this.config?.initialReconnectDelay ?? DEFAULT_INITIAL_RECONNECT_DELAY) * Math.pow(2, this.reconnectAttempts),
            this.config?.maxReconnectDelay ?? DEFAULT_MAX_RECONNECT_DELAY
        );

        console.log(`[AlertStream] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts + 1})`);

        this.reconnectTimer = setTimeout(() => {
            this.reconnectAttempts++;
            this.connectSSE();
        }, delay);
    }

    private startPolling(): void {
        if (this.pollingTimer) {
            return; // Already polling
        }

        console.log('[AlertStream] Starting polling fallback');
        this.isUsingFallback = true;
        this.updateConnectionStatus('connected');

        // Initial poll
        this.pollAlerts();

        // Set up interval
        this.pollingTimer = setInterval(() => {
            this.pollAlerts();
        }, this.config?.pollingInterval ?? DEFAULT_POLLING_INTERVAL);
    }

    private async pollAlerts(): Promise<void> {
        if (!this.config) return;

        const token = this.config.getAccessToken();
        if (!token) {
            console.warn('[AlertStream] No auth token for polling');
            return;
        }

        try {
            const url = new URL(`${API_BASE_URLS.PREDICTION}/alerts`);
            url.searchParams.set('unacknowledgedOnly', 'true');
            url.searchParams.set('size', '20');

            if (this.lastEventTimestamp) {
                url.searchParams.set('since', this.lastEventTimestamp);
            }

            const response = await fetch(url.toString(), {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });

            if (!response.ok) {
                throw new Error(`Polling failed: ${response.status}`);
            }

            const result = await response.json();

            if (result.success && result.data?.content) {
                const alerts = result.data.content as AlertSummary[];

                // Emit events for each alert
                for (const alert of alerts) {
                    const streamEvent: AlertStreamEvent = {
                        type: 'alert:new',
                        data: alert,
                        timestamp: new Date().toISOString(),
                    };
                    this.notifyEventSubscribers(streamEvent);
                }

                // Update last timestamp
                if (alerts.length > 0) {
                    this.lastEventTimestamp = alerts[0].createdAt;
                }
            }

        } catch (error) {
            console.error('[AlertStream] Polling error:', error);
            this.notifyErrorSubscribers(error as Error);
        }
    }

    private cleanup(): void {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }

        if (this.pollingTimer) {
            clearInterval(this.pollingTimer);
            this.pollingTimer = null;
        }

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }
    }

    private updateConnectionStatus(status: ConnectionStatus): void {
        if (this.connectionStatus !== status) {
            this.connectionStatus = status;
            this.notifyConnectionSubscribers(status);
        }
    }

    private notifyEventSubscribers(event: AlertStreamEvent): void {
        this.eventSubscribers.forEach((callback) => {
            try {
                callback(event);
            } catch (error) {
                console.error('[AlertStream] Subscriber error:', error);
            }
        });
    }

    private notifyConnectionSubscribers(status: ConnectionStatus): void {
        this.connectionSubscribers.forEach((callback) => {
            try {
                callback(status);
            } catch (error) {
                console.error('[AlertStream] Connection subscriber error:', error);
            }
        });
    }

    private notifyErrorSubscribers(error: Error): void {
        this.errorSubscribers.forEach((callback) => {
            try {
                callback(error);
            } catch (err) {
                console.error('[AlertStream] Error subscriber error:', err);
            }
        });
    }
}

// Export singleton getter
export const getAlertStreamManager = () => AlertStreamManager.getInstance();

// Export types for external use
export type { StreamConfig, AlertEventCallback, ConnectionCallback, ErrorCallback };
