import { Calendar } from 'lucide-react';

interface DateRangeFilterProps {
    fromDate: string | null;
    toDate: string | null;
    onFromDateChange: (date: string | null) => void;
    onToDateChange: (date: string | null) => void;
}

export function DateRangeFilter({
    fromDate,
    toDate,
    onFromDateChange,
    onToDateChange,
}: DateRangeFilterProps) {
    return (
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-2">
            <div className="relative w-full sm:w-auto">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                <input
                    type="date"
                    value={fromDate || ''}
                    onChange={(e) => onFromDateChange(e.target.value || null)}
                    className="input-field pl-10 w-full sm:w-40 text-sm"
                    placeholder="From date"
                />
            </div>
            <span className="text-gray-500 hidden sm:block">to</span>
            <div className="relative w-full sm:w-auto">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                <input
                    type="date"
                    value={toDate || ''}
                    onChange={(e) => onToDateChange(e.target.value || null)}
                    className="input-field pl-10 w-full sm:w-40 text-sm"
                    placeholder="To date"
                />
            </div>
        </div>
    );
}
