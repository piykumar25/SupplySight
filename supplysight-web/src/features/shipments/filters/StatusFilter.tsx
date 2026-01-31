import { useState, useRef, useEffect } from 'react';
import { Check, ChevronDown } from 'lucide-react';
import { ShipmentStatus } from '@/types/shipment.types';
import { SHIPMENT_STATUS_OPTIONS } from '../types';

interface StatusFilterProps {
    selected: ShipmentStatus[];
    onChange: (statuses: ShipmentStatus[]) => void;
}

export function StatusFilter({ selected, onChange }: StatusFilterProps) {
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    // Close on outside click
    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const toggleStatus = (status: ShipmentStatus) => {
        if (selected.includes(status)) {
            onChange(selected.filter((s) => s !== status));
        } else {
            onChange([...selected, status]);
        }
    };

    const getLabel = () => {
        if (selected.length === 0) return 'All Statuses';
        if (selected.length === 1) {
            return SHIPMENT_STATUS_OPTIONS.find((o) => o.value === selected[0])?.label;
        }
        return `${selected.length} selected`;
    };

    return (
        <div className="relative" ref={dropdownRef}>
            <button
                type="button"
                onClick={() => setIsOpen(!isOpen)}
                className="flex items-center justify-between gap-2 w-full md:w-48 px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-sm text-white hover:bg-white/10 transition-colors"
            >
                <span className="truncate">{getLabel()}</span>
                <ChevronDown className={`w-4 h-4 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
            </button>

            {isOpen && (
                <div className="absolute z-50 mt-1 w-full md:w-48 bg-surface border border-white/10 rounded-lg shadow-xl py-1 max-h-60 overflow-y-auto">
                    {SHIPMENT_STATUS_OPTIONS.map((option) => {
                        const isSelected = selected.includes(option.value);
                        return (
                            <button
                                key={option.value}
                                type="button"
                                onClick={() => toggleStatus(option.value)}
                                className="flex items-center gap-2 w-full px-3 py-2 text-sm text-left hover:bg-white/5 transition-colors"
                            >
                                <div
                                    className={`w-4 h-4 rounded border flex items-center justify-center transition-colors ${isSelected
                                            ? 'bg-primary border-primary'
                                            : 'border-white/20'
                                        }`}
                                >
                                    {isSelected && <Check className="w-3 h-3 text-white" />}
                                </div>
                                <span className={isSelected ? 'text-white' : 'text-gray-400'}>
                                    {option.label}
                                </span>
                            </button>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
