import { useState } from 'react';
import { ChevronDown, X, MapPin } from 'lucide-react';
import { StatusFilter, DateRangeFilter, RiskFilter } from '../filters';
import { ShipmentFilters, hasActiveFilters } from '../types';

interface FilterPanelProps {
    filters: ShipmentFilters;
    onFiltersChange: (filters: Partial<ShipmentFilters>) => void;
    onClearFilters: () => void;
}

export function FilterPanel({
    filters,
    onFiltersChange,
    onClearFilters,
}: FilterPanelProps) {
    const [isExpanded, setIsExpanded] = useState(false);
    const hasFilters = hasActiveFilters(filters);

    return (
        <div className="glass-panel overflow-hidden">
            {/* Header - Always visible */}
            <button
                type="button"
                onClick={() => setIsExpanded(!isExpanded)}
                className="w-full flex items-center justify-between px-4 py-3 hover:bg-white/5 transition-colors"
            >
                <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-white">Filters</span>
                    {hasFilters && (
                        <span className="px-2 py-0.5 text-xs font-medium bg-primary/20 text-primary rounded-full">
                            Active
                        </span>
                    )}
                </div>
                <ChevronDown
                    className={`w-4 h-4 text-gray-400 transition-transform ${isExpanded ? 'rotate-180' : ''
                        }`}
                />
            </button>

            {/* Expanded content */}
            {isExpanded && (
                <div className="px-4 pb-4 space-y-4 border-t border-white/5">
                    {/* Row 1: Status and Risk */}
                    <div className="flex flex-col md:flex-row gap-4 pt-4">
                        <div className="flex-1">
                            <label className="block text-xs font-medium text-gray-400 mb-1.5">
                                Status
                            </label>
                            <StatusFilter
                                selected={filters.statuses}
                                onChange={(statuses) => onFiltersChange({ statuses })}
                            />
                        </div>
                        <div className="flex-1">
                            <label className="block text-xs font-medium text-gray-400 mb-1.5">
                                Risk Level
                            </label>
                            <RiskFilter
                                value={filters.riskLevel}
                                onChange={(riskLevel) => onFiltersChange({ riskLevel })}
                            />
                        </div>
                    </div>

                    {/* Row 2: Date Range */}
                    <div>
                        <label className="block text-xs font-medium text-gray-400 mb-1.5">
                            Date Range
                        </label>
                        <DateRangeFilter
                            fromDate={filters.fromDate}
                            toDate={filters.toDate}
                            onFromDateChange={(fromDate) => onFiltersChange({ fromDate })}
                            onToDateChange={(toDate) => onFiltersChange({ toDate })}
                        />
                    </div>

                    {/* Row 3: Origin / Destination */}
                    <div className="flex flex-col md:flex-row gap-4">
                        <div className="flex-1">
                            <label className="block text-xs font-medium text-gray-400 mb-1.5">
                                Origin
                            </label>
                            <div className="relative">
                                <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                                <input
                                    type="text"
                                    value={filters.origin}
                                    onChange={(e) => onFiltersChange({ origin: e.target.value })}
                                    placeholder="Hub code or city..."
                                    className="input-field pl-10 text-sm"
                                />
                            </div>
                        </div>
                        <div className="flex-1">
                            <label className="block text-xs font-medium text-gray-400 mb-1.5">
                                Destination
                            </label>
                            <div className="relative">
                                <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                                <input
                                    type="text"
                                    value={filters.destination}
                                    onChange={(e) => onFiltersChange({ destination: e.target.value })}
                                    placeholder="Hub code or city..."
                                    className="input-field pl-10 text-sm"
                                />
                            </div>
                        </div>
                    </div>

                    {/* Clear Filters */}
                    {hasFilters && (
                        <div className="flex justify-end pt-2 border-t border-white/5">
                            <button
                                type="button"
                                onClick={onClearFilters}
                                className="flex items-center gap-1.5 text-sm text-gray-400 hover:text-white transition-colors"
                            >
                                <X className="w-4 h-4" />
                                Clear all filters
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
