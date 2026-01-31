import { RISK_LEVEL_OPTIONS, RiskLevel } from '../types';

interface RiskFilterProps {
    value: RiskLevel;
    onChange: (level: RiskLevel) => void;
}

export function RiskFilter({ value, onChange }: RiskFilterProps) {
    return (
        <div className="flex items-center gap-1 bg-white/5 rounded-lg p-1">
            {RISK_LEVEL_OPTIONS.map((option) => (
                <button
                    key={option.value}
                    type="button"
                    onClick={() => onChange(option.value)}
                    className={`px-3 py-1.5 text-sm rounded-md transition-colors ${value === option.value
                            ? 'bg-primary text-white'
                            : 'text-gray-400 hover:text-white hover:bg-white/5'
                        }`}
                >
                    {option.label === 'All Risk Levels' ? 'All' : option.label}
                </button>
            ))}
        </div>
    );
}
