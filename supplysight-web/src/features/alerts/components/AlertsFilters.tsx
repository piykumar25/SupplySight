/**
 * Alerts Filters Component
 * 
 * Filter controls for the alerts list:
 * - Severity multi-select
 * - Status multi-select
 * - Alert type filter
 * - Date range picker
 * - Free-text search
 * - Reset filters button
 */

import { useState, useCallback } from 'react';
import { Search, Filter, X, Calendar, ChevronDown } from 'lucide-react';
import type { AlertFilters } from '../types';
import {
    SEVERITY_OPTIONS,
    STATUS_OPTIONS,
    ALERT_TYPE_OPTIONS,
    DEFAULT_ALERT_FILTERS,
    hasActiveAlertFilters
} from '../types';

/**
 * Props for AlertsFilters
 */
interface AlertsFiltersProps {
    filters: AlertFilters;
    onFiltersChange: (filters: AlertFilters) => void;
}

/**
 * Multi-select dropdown component
 */
function MultiSelectDropdown<T extends string>({
    label,
    options,
    selected,
    onChange,
}: {
    label: string;
    options: { value: T; label: string }[];
    selected: T[];
    onChange: (values: T[]) => void;
}) {
    const [isOpen, setIsOpen] = useState(false);

    const toggleOption = (value: T) => {
        if (selected.includes(value)) {
            onChange(selected.filter((v) => v !== value));
        } else {
            onChange([...selected, value]);
        }
    };

    const selectedLabels = options
        .filter((o) => selected.includes(o.value))
        .map((o) => o.label);

    return (
        <div className="relative">
            <button
                onClick={() => setIsOpen(!isOpen)}
                className={`flex items-center gap-2 px-3 py-2 rounded-lg border transition-colors text-sm ${selected.length > 0
                    ? 'border-primary/50 bg-primary/10 text-white'
                    : 'border-white/10 bg-white/5 text-gray-400 hover:border-white/20'
                    }`}
            >
                <span>
                    {selected.length === 0
                        ? label
                        : selected.length === 1
                            ? selectedLabels[0]
                            : `${selected.length} selected`}
                </span>
                <ChevronDown className={`w-4 h-4 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
            </button>

            {isOpen && (
                <>
                    <div
                        className="fixed inset-0 z-10"
                        onClick={() => setIsOpen(false)}
                    />
                    <div className="absolute top-full left-0 mt-1 w-48 bg-surface border border-white/10 rounded-lg shadow-xl z-20 py-1">
                        {options.map((option) => (
                            <button
                                key={option.value}
                                onClick={() => toggleOption(option.value)}
                                className={`w-full flex items-center gap-2 px-3 py-2 text-sm text-left hover:bg-white/5 transition-colors ${selected.includes(option.value) ? 'text-primary' : 'text-gray-300'
                                    }`}
                            >
                                <input
                                    type="checkbox"
                                    checked={selected.includes(option.value)}
                                    readOnly
                                    className="w-4 h-4 rounded border-white/20 bg-white/5 text-primary focus:ring-0"
                                />
                                {option.label}
                            </button>
                        ))}
                    </div>
                </>
            )}
        </div>
    );
}

/**
 * Date range input
 */
function DateRangeInput({
    fromDate,
    toDate,
    onFromChange,
    onToChange,
}: {
    fromDate: string | null;
    toDate: string | null;
    onFromChange: (date: string | null) => void;
    onToChange: (date: string | null) => void;
}) {
    return (
        <div className="flex items-center gap-2">
            <div className="relative">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                <input
                    type="date"
                    value={fromDate ?? ''}
                    onChange={(e) => onFromChange(e.target.value || null)}
                    className="pl-9 pr-3 py-2 rounded-lg border border-white/10 bg-white/5 text-sm text-white focus:border-primary/50 focus:ring-0 focus:outline-none w-36"
                    placeholder="From"
                />
            </div>
            <span className="text-gray-500">–</span>
            <div className="relative">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                <input
                    type="date"
                    value={toDate ?? ''}
                    onChange={(e) => onToChange(e.target.value || null)}
                    className="pl-9 pr-3 py-2 rounded-lg border border-white/10 bg-white/5 text-sm text-white focus:border-primary/50 focus:ring-0 focus:outline-none w-36"
                    placeholder="To"
                />
            </div>
        </div>
    );
}

/**
 * Alerts Filters Component
 */
export function AlertsFilters({ filters, onFiltersChange }: AlertsFiltersProps) {
    const [showAdvanced, setShowAdvanced] = useState(false);
    const hasFilters = hasActiveAlertFilters(filters);

    // Update individual filter
    const updateFilter = useCallback(
        <K extends keyof AlertFilters>(key: K, value: AlertFilters[K]) => {
            onFiltersChange({ ...filters, [key]: value });
        },
        [filters, onFiltersChange]
    );

    // Reset all filters
    const resetFilters = useCallback(() => {
        onFiltersChange(DEFAULT_ALERT_FILTERS);
    }, [onFiltersChange]);

    return (
        <div className="space-y-4">
            {/* Main filter row */}
            <div className="flex flex-wrap items-center gap-3">
                {/* Search */}
                <div className="relative flex-1 min-w-[200px] max-w-md">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                    <input
                        type="text"
                        value={filters.search}
                        onChange={(e) => updateFilter('search', e.target.value)}
                        placeholder="Search alerts..."
                        className="w-full pl-10 pr-4 py-2 rounded-lg border border-white/10 bg-white/5 text-sm text-white placeholder-gray-500 focus:border-primary/50 focus:ring-0 focus:outline-none"
                    />
                    {filters.search && (
                        <button
                            onClick={() => updateFilter('search', '')}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-white"
                        >
                            <X className="w-4 h-4" />
                        </button>
                    )}
                </div>

                {/* Severity */}
                <MultiSelectDropdown
                    label="Severity"
                    options={SEVERITY_OPTIONS}
                    selected={filters.severities}
                    onChange={(values) => updateFilter('severities', values)}
                />

                {/* Status */}
                <MultiSelectDropdown
                    label="Status"
                    options={STATUS_OPTIONS}
                    selected={filters.statuses}
                    onChange={(values) => updateFilter('statuses', values)}
                />

                {/* Advanced toggle */}
                <button
                    onClick={() => setShowAdvanced(!showAdvanced)}
                    className={`flex items-center gap-2 px-3 py-2 rounded-lg border transition-colors text-sm ${showAdvanced
                        ? 'border-primary/50 bg-primary/10 text-white'
                        : 'border-white/10 bg-white/5 text-gray-400 hover:border-white/20'
                        }`}
                >
                    <Filter className="w-4 h-4" />
                    <span>More</span>
                </button>

                {/* Reset */}
                {hasFilters && (
                    <button
                        onClick={resetFilters}
                        className="flex items-center gap-1.5 px-3 py-2 text-sm text-gray-400 hover:text-white transition-colors"
                    >
                        <X className="w-4 h-4" />
                        <span>Reset</span>
                    </button>
                )}
            </div>

            {/* Advanced filters */}
            {showAdvanced && (
                <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-white/5">
                    {/* Alert Type */}
                    <MultiSelectDropdown
                        label="Alert Type"
                        options={ALERT_TYPE_OPTIONS}
                        selected={filters.alertTypes}
                        onChange={(values) => updateFilter('alertTypes', values)}
                    />

                    {/* Date Range */}
                    <DateRangeInput
                        fromDate={filters.fromDate}
                        toDate={filters.toDate}
                        onFromChange={(date) => updateFilter('fromDate', date)}
                        onToChange={(date) => updateFilter('toDate', date)}
                    />
                </div>
            )}

            {/* Active filters summary */}
            {hasFilters && (
                <div className="flex flex-wrap items-center gap-2">
                    <span className="text-xs text-gray-500">Active:</span>

                    {filters.severities.map((severity) => (
                        <span
                            key={severity}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs bg-white/10 text-gray-300"
                        >
                            {severity}
                            <button
                                onClick={() =>
                                    updateFilter(
                                        'severities',
                                        filters.severities.filter((s) => s !== severity)
                                    )
                                }
                                className="hover:text-white"
                            >
                                <X className="w-3 h-3" />
                            </button>
                        </span>
                    ))}

                    {filters.statuses.map((status) => (
                        <span
                            key={status}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs bg-white/10 text-gray-300"
                        >
                            {status}
                            <button
                                onClick={() =>
                                    updateFilter(
                                        'statuses',
                                        filters.statuses.filter((s) => s !== status)
                                    )
                                }
                                className="hover:text-white"
                            >
                                <X className="w-3 h-3" />
                            </button>
                        </span>
                    ))}

                    {filters.alertTypes.map((type) => (
                        <span
                            key={type}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs bg-white/10 text-gray-300"
                        >
                            {type.replace(/_/g, ' ')}
                            <button
                                onClick={() =>
                                    updateFilter(
                                        'alertTypes',
                                        filters.alertTypes.filter((t) => t !== type)
                                    )
                                }
                                className="hover:text-white"
                            >
                                <X className="w-3 h-3" />
                            </button>
                        </span>
                    ))}

                    {filters.search && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs bg-white/10 text-gray-300">
                            "{filters.search}"
                            <button
                                onClick={() => updateFilter('search', '')}
                                className="hover:text-white"
                            >
                                <X className="w-3 h-3" />
                            </button>
                        </span>
                    )}

                    {(filters.fromDate || filters.toDate) && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs bg-white/10 text-gray-300">
                            {filters.fromDate ?? '...'} – {filters.toDate ?? '...'}
                            <button
                                onClick={() => {
                                    updateFilter('fromDate', null);
                                    updateFilter('toDate', null);
                                }}
                                className="hover:text-white"
                            >
                                <X className="w-3 h-3" />
                            </button>
                        </span>
                    )}
                </div>
            )}
        </div>
    );
}
