import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
    ShipmentFilters,
    ShipmentSortConfig,
    ShipmentPagination,
    ShipmentQueryParams,
    DEFAULT_FILTERS,
    DEFAULT_SORT,
    DEFAULT_PAGINATION,
    SortableColumn,
    SortDirection,
    RiskLevel,
    PageSize,
    PAGE_SIZE_OPTIONS,
} from '../types';
import { ShipmentStatus } from '@/types/shipment.types';

/**
 * Parse URL search params into filter/sort/pagination state
 */
function parseParams(searchParams: URLSearchParams): {
    filters: ShipmentFilters;
    sort: ShipmentSortConfig;
    pagination: ShipmentPagination;
} {
    // Parse statuses (comma-separated)
    const statusesParam = searchParams.get('statuses');
    const statuses = statusesParam
        ? (statusesParam.split(',').filter(Boolean) as ShipmentStatus[])
        : [];

    // Parse dates
    const fromDate = searchParams.get('fromDate') || null;
    const toDate = searchParams.get('toDate') || null;

    // Parse text filters
    const origin = searchParams.get('origin') || '';
    const destination = searchParams.get('destination') || '';
    const search = searchParams.get('search') || '';

    // Parse risk level
    const riskLevelParam = searchParams.get('riskLevel');
    const riskLevel: RiskLevel =
        riskLevelParam && ['all', 'normal', 'delayed', 'high'].includes(riskLevelParam)
            ? (riskLevelParam as RiskLevel)
            : 'all';

    // Parse sort
    const sortByParam = searchParams.get('sortBy');
    const sortDirParam = searchParams.get('sortDir');
    const column: SortableColumn =
        sortByParam && ['status', 'updatedAt', 'eta', 'delayProbability'].includes(sortByParam)
            ? (sortByParam as SortableColumn)
            : DEFAULT_SORT.column;
    const direction: SortDirection =
        sortDirParam && ['asc', 'desc'].includes(sortDirParam)
            ? (sortDirParam as SortDirection)
            : DEFAULT_SORT.direction;

    // Parse pagination
    const pageParam = searchParams.get('page');
    const sizeParam = searchParams.get('size');
    const page = pageParam ? Math.max(0, parseInt(pageParam, 10) || 0) : 0;
    const sizeValue = sizeParam ? parseInt(sizeParam, 10) : DEFAULT_PAGINATION.size;
    const size = (PAGE_SIZE_OPTIONS as readonly number[]).includes(sizeValue)
        ? (sizeValue as PageSize)
        : DEFAULT_PAGINATION.size;

    return {
        filters: {
            statuses,
            fromDate,
            toDate,
            origin,
            destination,
            riskLevel,
            search,
        },
        sort: { column, direction },
        pagination: { page, size },
    };
}

/**
 * Convert state to URL search params
 */
function serializeParams(
    filters: ShipmentFilters,
    sort: ShipmentSortConfig,
    pagination: ShipmentPagination
): ShipmentQueryParams {
    const params: ShipmentQueryParams = {};

    // Serialize filters (only non-default values)
    if (filters.statuses.length > 0) {
        params.statuses = filters.statuses.join(',');
    }
    if (filters.fromDate) {
        params.fromDate = filters.fromDate;
    }
    if (filters.toDate) {
        params.toDate = filters.toDate;
    }
    if (filters.origin) {
        params.origin = filters.origin;
    }
    if (filters.destination) {
        params.destination = filters.destination;
    }
    if (filters.riskLevel !== 'all') {
        params.riskLevel = filters.riskLevel;
    }
    if (filters.search) {
        params.search = filters.search;
    }

    // Serialize sort (only non-default values)
    if (sort.column !== DEFAULT_SORT.column) {
        params.sortBy = sort.column;
    }
    if (sort.direction !== DEFAULT_SORT.direction) {
        params.sortDir = sort.direction;
    }

    // Serialize pagination (only non-default values)
    if (pagination.page !== 0) {
        params.page = String(pagination.page);
    }
    if (pagination.size !== DEFAULT_PAGINATION.size) {
        params.size = String(pagination.size);
    }

    return params;
}

/**
 * Hook to sync shipment filters, sorting, and pagination with URL query params
 */
export function useShipmentUrlParams() {
    const [searchParams, setSearchParams] = useSearchParams();

    // Parse current state from URL
    const { filters, sort, pagination } = useMemo(
        () => parseParams(searchParams),
        [searchParams]
    );

    // Update URL with new state
    const updateParams = useCallback(
        (
            newFilters?: Partial<ShipmentFilters>,
            newSort?: Partial<ShipmentSortConfig>,
            newPagination?: Partial<ShipmentPagination>
        ) => {
            const updatedFilters = { ...filters, ...newFilters };
            const updatedSort = { ...sort, ...newSort };
            const updatedPagination = { ...pagination, ...newPagination };

            // Reset page when filters or sort change
            if (newFilters || newSort) {
                updatedPagination.page = 0;
            }

            const params = serializeParams(updatedFilters, updatedSort, updatedPagination);

            // Convert to URLSearchParams format
            const newSearchParams = new URLSearchParams();
            Object.entries(params).forEach(([key, value]) => {
                if (value !== undefined) {
                    newSearchParams.set(key, value);
                }
            });

            setSearchParams(newSearchParams, { replace: true });
        },
        [filters, sort, pagination, setSearchParams]
    );

    // Convenience methods
    const setFilters = useCallback(
        (newFilters: Partial<ShipmentFilters>) => updateParams(newFilters),
        [updateParams]
    );

    const setSort = useCallback(
        (newSort: Partial<ShipmentSortConfig>) => updateParams(undefined, newSort),
        [updateParams]
    );

    const setPagination = useCallback(
        (newPagination: Partial<ShipmentPagination>) => updateParams(undefined, undefined, newPagination),
        [updateParams]
    );

    const setPage = useCallback(
        (page: number) => setPagination({ page }),
        [setPagination]
    );

    const setPageSize = useCallback(
        (size: PageSize) => setPagination({ size, page: 0 }),
        [setPagination]
    );

    const toggleSort = useCallback(
        (column: SortableColumn) => {
            if (sort.column === column) {
                // Toggle direction if same column
                setSort({ direction: sort.direction === 'asc' ? 'desc' : 'asc' });
            } else {
                // Set new column with default desc
                setSort({ column, direction: 'desc' });
            }
        },
        [sort, setSort]
    );

    const clearFilters = useCallback(() => {
        updateParams(DEFAULT_FILTERS);
    }, [updateParams]);

    return {
        filters,
        sort,
        pagination,
        setFilters,
        setSort,
        setPagination,
        setPage,
        setPageSize,
        toggleSort,
        clearFilters,
    };
}
