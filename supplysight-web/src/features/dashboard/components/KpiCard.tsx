import { TrendingUp } from 'lucide-react';

export interface KpiCardProps {
    title: string;
    value: string | number;
    subtitle?: string;
    icon: React.ElementType;
    trend?: 'up' | 'down' | 'neutral';
    trendValue?: string;
    color: 'primary' | 'green' | 'yellow' | 'red';
}

const colorClasses = {
    primary: 'from-primary/20 to-blue-600/20 border-primary/30 shadow-primary/10',
    green: 'from-green-500/20 to-emerald-600/20 border-green-500/30 shadow-green-500/10',
    yellow: 'from-yellow-500/20 to-amber-600/20 border-yellow-500/30 shadow-yellow-500/10',
    red: 'from-red-500/20 to-rose-600/20 border-red-500/30 shadow-red-500/10',
};

const iconColorClasses = {
    primary: 'text-primary',
    green: 'text-green-400',
    yellow: 'text-yellow-400',
    red: 'text-red-400',
};

/**
 * KPI Card component for displaying key performance indicators
 */
export function KpiCard({ title, value, subtitle, icon: Icon, trend, trendValue, color }: KpiCardProps) {
    return (
        <div className={`glass-panel p-6 bg-gradient-to-br ${colorClasses[color]} shadow-lg min-h-[140px]`}>
            <div className="flex items-start justify-between mb-4">
                <div className={`p-3 rounded-xl bg-white/10 ${iconColorClasses[color]}`}>
                    <Icon className="w-6 h-6" />
                </div>
                {trend && trendValue && (
                    <div className={`flex items-center gap-1 text-sm ${
                        trend === 'up' ? 'text-green-400' : 
                        trend === 'down' ? 'text-red-400' : 'text-gray-400'
                    }`}>
                        <TrendingUp className={`w-4 h-4 ${trend === 'down' ? 'rotate-180' : ''}`} />
                        <span>{trendValue}</span>
                    </div>
                )}
            </div>
            <p className="text-3xl font-bold text-white mb-1">{value}</p>
            <p className="text-sm text-gray-400">{title}</p>
            {subtitle && <p className="text-xs text-gray-500 mt-1">{subtitle}</p>}
        </div>
    );
}
