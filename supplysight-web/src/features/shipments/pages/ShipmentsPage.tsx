import { RefreshCw } from 'lucide-react';
import { useShipmentUrlParams } from '../hooks/useUrlParams';
import { useShipmentsQuery } from '../queries';
import { PageSize } from '../types';
import {
    FilterPanel,
    SearchInput,
    ShipmentsTable,
    PaginationControls,
    ExportButton,
} from '../components';

/**
 * Shipments Page - Enterprise Search Console
 * URL-driven state for filters, sorting, and pagination
 */
export function ShipmentsPage() {
    // URL-synced state
    const {
        filters,
        sort,
        pagination,
        setFilters,
        setPage,
        setPageSize,
        toggleSort,
        clearFilters,
    } = useShipmentUrlParams();

    // Query with URL params
    const { data, isLoading, isError, error, refetch, isFetching } = useShipmentsQuery({
        filters,
        sort,
        pagination,
    });

    return (
        <div className="space-y-4">
            {/* Header */}
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-white">Shipments</h1>
                    <p className="text-gray-400">Track and manage all your shipments</p>
                </div>
                <div className="flex items-center gap-3">
                    <SearchInput
                        value={filters.search}
                        onChange={(search) => setFilters({ search })}
                    />
                    <ExportButton
                        shipments={data?.content || []}
                        disabled={isLoading || !data?.content.length}
                    />
                </div>
            </div>

            {/* Filter Panel */}
            <FilterPanel
                filters={filters}
                onFiltersChange={setFilters}
                onClearFilters={clearFilters}
            />

            {/* Error State */}
            {isError && (
                <div className="glass-panel p-6 text-center">
                    <div className="text-red-400 mb-4">
                        <p className="text-lg font-medium">Failed to load shipments</p>
                        <p className="text-sm text-gray-400 mt-1">
                            {error instanceof Error ? error.message : 'An unexpected error occurred'}
                        </p>
                    </div>
                    <button
                        onClick={() => refetch()}
                        className="btn-primary flex items-center gap-2 mx-auto"
                    >
                        <RefreshCw className="w-4 h-4" />
                        Retry
                    </button>
                </div>
            )}

            {/* Main Table */}
            {!isError && (
                <div className="glass-panel p-4 lg:p-6">
                    <ShipmentsTable
                        data={data?.content || []}
                        isLoading={isLoading}
                        sortColumn={sort.column}
                        sortDirection={sort.direction}
                        onSort={toggleSort}
                    />

                    {/* Pagination */}
                    {data && data.totalElements > 0 && (
                        <PaginationControls
                            page={pagination.page}
                            pageSize={pagination.size as PageSize}
                            totalElements={data.totalElements}
                            totalPages={data.totalPages}
                            onPageChange={setPage}
                            onPageSizeChange={setPageSize}
                            isLoading={isFetching}
                        />
                    )}
                </div>
            )}
        </div>
    );
}
