import { useState } from 'react';
import {
    AreaChart,
    Area,
    BarChart,
    Bar,
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer
} from 'recharts';
import { Calendar, TrendingUp, AlertTriangle } from 'lucide-react';

// Generate mock trend data (to be replaced with real API data)
function generateTrendData(days: number) {
    const data = [];
    const now = new Date();

    for (let i = days - 1; i >= 0; i--) {
        const date = new Date(now);
        date.setDate(date.getDate() - i);

        data.push({
            date: date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
            shipments: Math.floor(Math.random() * 50) + 20,
            delayed: Math.floor(Math.random() * 10) + 2,
            delayRate: Math.round((Math.random() * 15 + 5)),
            anomalies: Math.floor(Math.random() * 5),
        });
    }

    return data;
}

type TimeRange = '7d' | '30d';

interface TrendsPanelProps {
    className?: string;
}

/**
 * Trends Panel with interactive charts
 * Shows shipment volume, delay rates, and anomaly frequency
 */
export function TrendsPanel({ className }: TrendsPanelProps) {
    const [timeRange, setTimeRange] = useState<TimeRange>('7d');
    const days = timeRange === '7d' ? 7 : 30;
    const data = generateTrendData(days);

    // Calculate summary stats
    const totalShipments = data.reduce((sum, d) => sum + d.shipments, 0);
    const avgDelayRate = Math.round(data.reduce((sum, d) => sum + d.delayRate, 0) / data.length);
    const totalAnomalies = data.reduce((sum, d) => sum + d.anomalies, 0);

    return (
        <div className={`glass-panel p-6 ${className}`}>
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
                <h2 className="text-lg font-semibold text-white flex items-center gap-2">
                    <TrendingUp className="w-5 h-5 text-primary" />
                    Trends & Analytics
                </h2>
                <div className="flex items-center gap-2">
                    <Calendar className="w-4 h-4 text-gray-400" />
                    <div className="flex bg-white/10 rounded-lg p-1">
                        <button
                            onClick={() => setTimeRange('7d')}
                            className={`px-3 py-1 rounded-md text-sm transition-colors ${timeRange === '7d'
                                ? 'bg-primary text-white'
                                : 'text-gray-400 hover:text-white'
                                }`}
                        >
                            7 Days
                        </button>
                        <button
                            onClick={() => setTimeRange('30d')}
                            className={`px-3 py-1 rounded-md text-sm transition-colors ${timeRange === '30d'
                                ? 'bg-primary text-white'
                                : 'text-gray-400 hover:text-white'
                                }`}
                        >
                            30 Days
                        </button>
                    </div>
                </div>
            </div>

            {/* Summary Stats */}
            <div className="grid grid-cols-3 gap-4 mb-6">
                <div className="text-center p-3 rounded-lg bg-white/5">
                    <p className="text-2xl font-bold text-white">{totalShipments}</p>
                    <p className="text-xs text-gray-400">Total Shipments</p>
                </div>
                <div className="text-center p-3 rounded-lg bg-white/5">
                    <p className="text-2xl font-bold text-yellow-400">{avgDelayRate}%</p>
                    <p className="text-xs text-gray-400">Avg Delay Rate</p>
                </div>
                <div className="text-center p-3 rounded-lg bg-white/5">
                    <p className="text-2xl font-bold text-red-400">{totalAnomalies}</p>
                    <p className="text-xs text-gray-400">Anomalies</p>
                </div>
            </div>

            {/* Charts Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Shipments Bar Chart */}
                <div>
                    <h3 className="text-sm font-medium text-gray-400 mb-4">Shipments per Day</h3>
                    <div className="h-[200px]">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={data}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                                <XAxis
                                    dataKey="date"
                                    tick={{ fill: '#9CA3AF', fontSize: 10 }}
                                    tickLine={{ stroke: '#374151' }}
                                    axisLine={{ stroke: '#374151' }}
                                />
                                <YAxis
                                    tick={{ fill: '#9CA3AF', fontSize: 10 }}
                                    tickLine={{ stroke: '#374151' }}
                                    axisLine={{ stroke: '#374151' }}
                                />
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: '#1F2937',
                                        border: '1px solid #374151',
                                        borderRadius: '8px',
                                        color: '#fff'
                                    }}
                                />
                                <Bar dataKey="shipments" fill="#3B82F6" radius={[4, 4, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* Delay Rate Line Chart */}
                <div>
                    <h3 className="text-sm font-medium text-gray-400 mb-4">Delay Rate Trend (%)</h3>
                    <div className="h-[200px]">
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={data}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                                <XAxis
                                    dataKey="date"
                                    tick={{ fill: '#9CA3AF', fontSize: 10 }}
                                    tickLine={{ stroke: '#374151' }}
                                    axisLine={{ stroke: '#374151' }}
                                />
                                <YAxis
                                    tick={{ fill: '#9CA3AF', fontSize: 10 }}
                                    tickLine={{ stroke: '#374151' }}
                                    axisLine={{ stroke: '#374151' }}
                                />
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: '#1F2937',
                                        border: '1px solid #374151',
                                        borderRadius: '8px',
                                        color: '#fff'
                                    }}
                                />
                                <Line
                                    type="monotone"
                                    dataKey="delayRate"
                                    stroke="#F59E0B"
                                    strokeWidth={2}
                                    dot={{ fill: '#F59E0B', strokeWidth: 2 }}
                                />
                            </LineChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* Anomaly Area Chart */}
                <div className="lg:col-span-2">
                    <h3 className="text-sm font-medium text-gray-400 mb-4 flex items-center gap-2">
                        <AlertTriangle className="w-4 h-4 text-red-400" />
                        Anomaly Frequency
                    </h3>
                    <div className="h-[150px]">
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={data}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                                <XAxis
                                    dataKey="date"
                                    tick={{ fill: '#9CA3AF', fontSize: 10 }}
                                    tickLine={{ stroke: '#374151' }}
                                    axisLine={{ stroke: '#374151' }}
                                />
                                <YAxis
                                    tick={{ fill: '#9CA3AF', fontSize: 10 }}
                                    tickLine={{ stroke: '#374151' }}
                                    axisLine={{ stroke: '#374151' }}
                                />
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: '#1F2937',
                                        border: '1px solid #374151',
                                        borderRadius: '8px',
                                        color: '#fff'
                                    }}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="anomalies"
                                    stroke="#EF4444"
                                    fill="url(#anomalyGradient)"
                                    strokeWidth={2}
                                />
                                <defs>
                                    <linearGradient id="anomalyGradient" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#EF4444" stopOpacity={0.3} />
                                        <stop offset="95%" stopColor="#EF4444" stopOpacity={0} />
                                    </linearGradient>
                                </defs>
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </div>
        </div>
    );
}
