import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';
import { PAGE_SIZE_OPTIONS, PageSize } from '../types';

interface PaginationControlsProps {
    page: number;
    pageSize: PageSize;
    totalElements: number;
    totalPages: number;
    onPageChange: (page: number) => void;
    onPageSizeChange: (size: PageSize) => void;
    isLoading?: boolean;
}

export function PaginationControls({
    page,
    pageSize,
    totalElements,
    totalPages,
    onPageChange,
    onPageSizeChange,
    isLoading,
}: PaginationControlsProps) {
    const startItem = page * pageSize + 1;
    const endItem = Math.min((page + 1) * pageSize, totalElements);

    return (
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 py-4 px-2">
            {/* Results info */}
            <div className="flex items-center gap-4">
                <span className="text-sm text-gray-400">
                    Showing{' '}
                    <span className="text-white font-medium">
                        {totalElements > 0 ? startItem : 0}-{endItem}
                    </span>{' '}
                    of <span className="text-white font-medium">{totalElements.toLocaleString()}</span> results
                </span>

                {/* Page size selector */}
                <div className="flex items-center gap-2">
                    <span className="text-sm text-gray-400">Per page:</span>
                    <select
                        value={pageSize}
                        onChange={(e) => onPageSizeChange(Number(e.target.value) as PageSize)}
                        className="bg-white/5 border border-white/10 rounded-lg px-2 py-1 text-sm text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                        disabled={isLoading}
                    >
                        {PAGE_SIZE_OPTIONS.map((size) => (
                            <option key={size} value={size} className="bg-surface">
                                {size}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {/* Page navigation */}
            <div className="flex items-center gap-1">
                {/* First page */}
                <button
                    onClick={() => onPageChange(0)}
                    disabled={page === 0 || isLoading}
                    className="p-2 rounded-lg hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    title="First page"
                >
                    <ChevronsLeft className="w-4 h-4" />
                </button>

                {/* Previous page */}
                <button
                    onClick={() => onPageChange(page - 1)}
                    disabled={page === 0 || isLoading}
                    className="p-2 rounded-lg hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    title="Previous page"
                >
                    <ChevronLeft className="w-4 h-4" />
                </button>

                {/* Page indicator */}
                <span className="px-4 py-1 text-sm">
                    <span className="text-white font-medium">{page + 1}</span>
                    <span className="text-gray-400"> / </span>
                    <span className="text-gray-400">{Math.max(1, totalPages)}</span>
                </span>

                {/* Next page */}
                <button
                    onClick={() => onPageChange(page + 1)}
                    disabled={page >= totalPages - 1 || isLoading}
                    className="p-2 rounded-lg hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    title="Next page"
                >
                    <ChevronRight className="w-4 h-4" />
                </button>

                {/* Last page */}
                <button
                    onClick={() => onPageChange(totalPages - 1)}
                    disabled={page >= totalPages - 1 || isLoading}
                    className="p-2 rounded-lg hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    title="Last page"
                >
                    <ChevronsRight className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
}
