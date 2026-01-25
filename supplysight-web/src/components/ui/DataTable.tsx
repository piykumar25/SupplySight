import { ReactNode } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export interface Column<T> {
    header: string;
    accessorKey?: keyof T;
    cell?: (item: T) => ReactNode;
    className?: string;
}

interface DataTableProps<T> {
    data: T[];
    columns: Column<T>[];
    isLoading?: boolean;
    pagination?: {
        page: number;
        totalPages: number;
        onPageChange: (page: number) => void;
        totalElements: number;
    };
    onRowClick?: (item: T) => void;
}

export function DataTable<T>({
    data,
    columns,
    isLoading,
    pagination,
    onRowClick
}: DataTableProps<T>) {
    if (isLoading) {
        return (
            <div className="w-full h-64 flex items-center justify-center bg-black/20 rounded-lg border border-white/5">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
        );
    }

    if (!data.length) {
        return (
            <div className="w-full h-64 flex flex-col items-center justify-center bg-black/20 rounded-lg border border-white/5 text-gray-500">
                <p>No data found</p>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            <div className="overflow-x-auto rounded-lg border border-white/10 bg-black/20">
                <table className="w-full text-left">
                    <thead className="bg-white/5 text-gray-400 text-xs uppercase">
                        <tr>
                            {columns.map((col, index) => (
                                <th key={index} className={`px-6 py-3 font-medium ${col.className || ''}`}>
                                    {col.header}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                        {data.map((item, rowIndex) => (
                            <tr
                                key={rowIndex}
                                className={`
                                    hover:bg-white/5 transition-colors
                                    ${onRowClick ? 'cursor-pointer' : ''}
                                `}
                                onClick={() => onRowClick && onRowClick(item)}
                            >
                                {columns.map((col, colIndex) => (
                                    <td key={colIndex} className={`px-6 py-4 text-sm text-gray-300 ${col.className || ''}`}>
                                        {col.cell
                                            ? col.cell(item)
                                            : col.accessorKey
                                                ? String(item[col.accessorKey])
                                                : ''
                                        }
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Pagination Controls */}
            {pagination && pagination.totalPages > 1 && (
                <div className="flex items-center justify-between text-sm text-gray-400">
                    <div>
                        Showing <span className="text-white">{data.length}</span> of <span className="text-white">{pagination.totalElements}</span> results
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            className="p-2 rounded hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                            onClick={() => pagination.onPageChange(pagination.page - 1)}
                            disabled={pagination.page === 0}
                        >
                            <ChevronLeft className="w-4 h-4" />
                        </button>
                        <span className="text-white">
                            Page {pagination.page + 1} of {pagination.totalPages}
                        </span>
                        <button
                            className="p-2 rounded hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                            onClick={() => pagination.onPageChange(pagination.page + 1)}
                            disabled={pagination.page >= pagination.totalPages - 1}
                        >
                            <ChevronRight className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
