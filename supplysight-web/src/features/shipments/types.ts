import { ShipmentStatus } from '@/types/shipment.types';

/**
 * Risk level for filtering shipments
 */
export type RiskLevel = 'all' | 'normal' | 'delayed' | 'high';

/**
 * Sortable column keys
 */
export type SortableColumn = 'status' | 'updatedAt' | 'eta' | 'delayProbability';

/**
 * Sort direction
 */
export type SortDirection = 'asc' | 'desc';

/**
 * Filter state for shipments
 */
export interface ShipmentFilters {
    /** Multi-select status filter */
    statuses: ShipmentStatus[];
    /** Date range - from date (ISO string) */
    fromDate: string | null;
    /** Date range - to date (ISO string) */
    toDate: string | null;
    /** Origin hub code or address search */
    origin: string;
    /** Destination hub code or address search */
    destination: string;
    /** Risk level filter */
    riskLevel: RiskLevel;
    /** Free-text search (tracking number, shipment ID) */
    search: string;
}

/**
 * Sort configuration
 */
export interface ShipmentSortConfig {
    column: SortableColumn;
    direction: SortDirection;
}

/**
 * Pagination state
 */
export interface ShipmentPagination {
    page: number;
    size: number;
}

/**
 * Page size options
 */
export const PAGE_SIZE_OPTIONS = [25, 50, 100] as const;
export type PageSize = (typeof PAGE_SIZE_OPTIONS)[number];

/**
 * All query params serializable to URL
 */
export interface ShipmentQueryParams {
    // Filters
    statuses?: string;  // comma-separated
    fromDate?: string;
    toDate?: string;
    origin?: string;
    destination?: string;
    riskLevel?: RiskLevel;
    search?: string;
    // Pagination
    page?: string;
    size?: string;
    // Sorting
    sortBy?: SortableColumn;
    sortDir?: SortDirection;
}

/**
 * Default filter values
 */
export const DEFAULT_FILTERS: ShipmentFilters = {
    statuses: [],
    fromDate: null,
    toDate: null,
    origin: '',
    destination: '',
    riskLevel: 'all',
    search: '',
};

/**
 * Default sort config
 */
export const DEFAULT_SORT: ShipmentSortConfig = {
    column: 'updatedAt',
    direction: 'desc',
};

/**
 * Default pagination
 */
export const DEFAULT_PAGINATION: ShipmentPagination = {
    page: 0,
    size: 25,
};

/**
 * All shipment statuses for filter options
 */
export const SHIPMENT_STATUS_OPTIONS: { value: ShipmentStatus; label: string }[] = [
    { value: 'CREATED', label: 'Created' },
    { value: 'PICKED_UP', label: 'Picked Up' },
    { value: 'IN_TRANSIT', label: 'In Transit' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'DELAYED', label: 'Delayed' },
    { value: 'EXCEPTION', label: 'Exception' },
];

/**
 * Risk level options for filter
 */
export const RISK_LEVEL_OPTIONS: { value: RiskLevel; label: string }[] = [
    { value: 'all', label: 'All Risk Levels' },
    { value: 'normal', label: 'Normal' },
    { value: 'delayed', label: 'Delayed' },
    { value: 'high', label: 'High Risk' },
];

/**
 * Calculate risk level from delay probability
 */
export function getRiskLevel(delayProbability: number | undefined): RiskLevel {
    if (delayProbability === undefined) return 'normal';
    if (delayProbability > 0.8) return 'high';
    if (delayProbability > 0.5) return 'delayed';
    return 'normal';
}

/**
 * Check if filters are active (not default)
 */
export function hasActiveFilters(filters: ShipmentFilters): boolean {
    return (
        filters.statuses.length > 0 ||
        filters.fromDate !== null ||
        filters.toDate !== null ||
        filters.origin !== '' ||
        filters.destination !== '' ||
        filters.riskLevel !== 'all' ||
        filters.search !== ''
    );
}
