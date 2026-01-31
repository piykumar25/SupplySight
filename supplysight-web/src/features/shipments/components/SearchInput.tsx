import { useState, useEffect, useCallback } from 'react';
import { Search } from 'lucide-react';

interface SearchInputProps {
    value: string;
    onChange: (value: string) => void;
    placeholder?: string;
    debounceMs?: number;
}

export function SearchInput({
    value,
    onChange,
    placeholder = 'Search by tracking number or ID...',
    debounceMs = 300,
}: SearchInputProps) {
    const [localValue, setLocalValue] = useState(value);

    // Sync local value with prop when prop changes externally
    useEffect(() => {
        setLocalValue(value);
    }, [value]);

    // Debounced onChange
    useEffect(() => {
        const timer = setTimeout(() => {
            if (localValue !== value) {
                onChange(localValue);
            }
        }, debounceMs);

        return () => clearTimeout(timer);
    }, [localValue, value, onChange, debounceMs]);

    const handleChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        setLocalValue(e.target.value);
    }, []);

    const handleClear = useCallback(() => {
        setLocalValue('');
        onChange('');
    }, [onChange]);

    return (
        <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
            <input
                type="text"
                value={localValue}
                onChange={handleChange}
                placeholder={placeholder}
                className="input-field pl-10 pr-8 w-full md:w-80"
            />
            {localValue && (
                <button
                    type="button"
                    onClick={handleClear}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-white transition-colors"
                >
                    ×
                </button>
            )}
        </div>
    );
}
