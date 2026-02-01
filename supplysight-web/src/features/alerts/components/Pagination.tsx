/**
 * Pagination Component
 * 
 * Pagination controls for the alerts table:
 * - Page navigation
 * - Page size selector
 * - Results count display
 */

import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';
import type { AlertPagination } from '../types';

/**
 * Props for Pagination
 */
interface PaginationProps {
    pagination: AlertPagination;
    totalItems: number;
    totalPages: number;
    onPageChange: (page: number) => void;
    onPageSizeChange: (size: number) => void;
}

/**
 * Page size options
 */
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

/**
 * Pagination Component
 */
export function Pagination({
    pagination,
    totalItems,
    totalPages,
    onPageChange,
    onPageSizeChange,
}: PaginationProps) {
    const { page, size } = pagination;
    const startItem = page * size + 1;
    const endItem = Math.min((page + 1) * size, totalItems);

    // Calculate visible page numbers
    const getVisiblePages = () => {
        const pages: (number | 'ellipsis')[] = [];
        const maxVisible = 5;

        if (totalPages <= maxVisible) {
            for (let i = 0; i < totalPages; i++) {
                pages.push(i);
            }
        } else {
            // Always show first page
            pages.push(0);

            if (page > 2) {
                pages.push('ellipsis');
            }

            // Show pages around current
            const start = Math.max(1, page - 1);
            const end = Math.min(totalPages - 2, page + 1);

            for (let i = start; i <= end; i++) {
                pages.push(i);
            }

            if (page < totalPages - 3) {
                pages.push('ellipsis');
            }

            // Always show last page
            pages.push(totalPages - 1);
        }

        return pages;
    };

    if (totalItems === 0) {
        return null;
    }

    return (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 text-sm">
            {/* Results info */}
            <div className="text-gray-400">
                Showing <span className="text-white font-medium">{startItem}</span>
                {' – '}
                <span className="text-white font-medium">{endItem}</span>
                {' of '}
                <span className="text-white font-medium">{totalItems}</span>
                {' alerts'}
            </div>

            <div className="flex items-center gap-4">
                {/* Page size selector */}
                <div className="flex items-center gap-2">
                    <span className="text-gray-500">Per page:</span>
                    <select
                        value={size}
                        onChange={(e) => onPageSizeChange(Number(e.target.value))}
                        className="px-2 py-1 rounded border border-white/10 bg-white/5 text-white focus:border-primary/50 focus:ring-0 focus:outline-none"
                    >
                        {PAGE_SIZE_OPTIONS.map((opt) => (
                            <option key={opt} value={opt} className="bg-surface">
                                {opt}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Page navigation */}
                <div className="flex items-center gap-1">
                    {/* First page */}
                    <button
                        onClick={() => onPageChange(0)}
                        disabled={page === 0}
                        className="p-1.5 rounded-lg hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                        aria-label="First page"
                    >
                        <ChevronsLeft className="w-4 h-4" />
                    </button>

                    {/* Previous page */}
                    <button
                        onClick={() => onPageChange(page - 1)}
                        disabled={page === 0}
                        className="p-1.5 rounded-lg hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                        aria-label="Previous page"
                    >
                        <ChevronLeft className="w-4 h-4" />
                    </button>

                    {/* Page numbers */}
                    <div className="flex items-center gap-1 mx-1">
                        {getVisiblePages().map((p, idx) =>
                            p === 'ellipsis' ? (
                                <span key={`ellipsis-${idx}`} className="px-2 text-gray-500">
                                    ...
                                </span>
                            ) : (
                                <button
                                    key={p}
                                    onClick={() => onPageChange(p)}
                                    className={`min-w-[32px] h-8 px-2 rounded-lg transition-colors ${p === page
                                            ? 'bg-primary text-white font-medium'
                                            : 'hover:bg-white/10 text-gray-400'
                                        }`}
                                >
                                    {p + 1}
                                </button>
                            )
                        )}
                    </div>

                    {/* Next page */}
                    <button
                        onClick={() => onPageChange(page + 1)}
                        disabled={page >= totalPages - 1}
                        className="p-1.5 rounded-lg hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                        aria-label="Next page"
                    >
                        <ChevronRight className="w-4 h-4" />
                    </button>

                    {/* Last page */}
                    <button
                        onClick={() => onPageChange(totalPages - 1)}
                        disabled={page >= totalPages - 1}
                        className="p-1.5 rounded-lg hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                        aria-label="Last page"
                    >
                        <ChevronsRight className="w-4 h-4" />
                    </button>
                </div>
            </div>
        </div>
    );
}
