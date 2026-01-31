import { ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { SortableColumn, SortDirection } from '../types';

interface SortableHeaderProps {
    column: SortableColumn;
    label: string;
    currentColumn: SortableColumn;
    currentDirection: SortDirection;
    onSort: (column: SortableColumn) => void;
}

export function SortableHeader({
    column,
    label,
    currentColumn,
    currentDirection,
    onSort,
}: SortableHeaderProps) {
    const isActive = currentColumn === column;

    return (
        <button
            type="button"
            onClick={() => onSort(column)}
            className="flex items-center gap-1 text-left hover:text-white transition-colors group"
        >
            <span>{label}</span>
            <span className="text-gray-500 group-hover:text-gray-300">
                {isActive ? (
                    currentDirection === 'asc' ? (
                        <ArrowUp className="w-4 h-4 text-primary" />
                    ) : (
                        <ArrowDown className="w-4 h-4 text-primary" />
                    )
                ) : (
                    <ArrowUpDown className="w-4 h-4" />
                )}
            </span>
        </button>
    );
}
