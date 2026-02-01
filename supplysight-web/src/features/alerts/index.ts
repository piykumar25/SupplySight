// Pages
export { AlertsPage } from './pages/AlertsPage';

// Components
export {
    NotificationBell,
    AlertToastContainer,
    AlertsListTable,
    AlertsFilters,
    Pagination,
    BulkActions,
    AlertDetailModal,
    AlertsErrorBoundary,
} from './components';

// Hooks
export { useAlertStream, useAlertStreamDemo } from './hooks';

// Queries
export {
    alertKeys,
    useAlertsQuery,
    useAlertDetailQuery,
    useAlertCountQuery,
    useShipmentAlertsQuery,
    useAcknowledgeAlertMutation,
    useResolveAlertMutation,
    useBulkAcknowledgeMutation,
    useBulkResolveMutation,
    usePrefetchAlertCount,
} from './queries';
export type { AlertQueryConfig } from './queries';

// Store
export {
    useAlertsStore,
    useUnreadCount,
    useRecentAlerts,
    useToastQueue,
    useConnectionStatus,
    requestNotificationPermission,
} from './store';

// Stream
export { getAlertStreamManager } from './stream';

// Types
export type {
    Alert,
    AlertSummary,
    AlertType,
    AlertSeverity,
    AlertStatus,
    AlertCount,
    AlertFilters,
    AlertSortConfig,
    AlertPagination,
    AlertStreamEvent,
    ConnectionStatus,
} from './types';

export {
    DEFAULT_ALERT_FILTERS,
    DEFAULT_ALERT_SORT,
    DEFAULT_ALERT_PAGINATION,
    SEVERITY_OPTIONS,
    STATUS_OPTIONS,
    ALERT_TYPE_OPTIONS,
    HIGH_SEVERITY_TYPES,
    isHighSeverityAlert,
    getAlertStatus,
    hasActiveAlertFilters,
} from './types';

