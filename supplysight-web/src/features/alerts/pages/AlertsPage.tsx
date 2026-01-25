import { AlertTriangle, Filter, Bell } from 'lucide-react';

/**
 * Alerts Page
 * Placeholder for Phase 5 implementation
 */
export function AlertsPage() {
    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-white">Alerts & Exceptions</h1>
                    <p className="text-gray-400">Monitor delay risks and anomalies</p>
                </div>
                <div className="flex items-center gap-3">
                    <button className="btn-secondary flex items-center gap-2" disabled>
                        <Filter className="w-4 h-4" />
                        <span>Filter by Severity</span>
                    </button>
                </div>
            </div>

            {/* Alert types summary */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="glass-panel p-4 border-l-4 border-red-500">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-400">DELAY_RISK_HIGH</p>
                            <p className="text-2xl font-bold text-white">8</p>
                        </div>
                        <div className="p-2 rounded-lg bg-red-500/20">
                            <AlertTriangle className="w-6 h-6 text-red-400" />
                        </div>
                    </div>
                </div>
                <div className="glass-panel p-4 border-l-4 border-yellow-500">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-400">ANOMALY_DETECTED</p>
                            <p className="text-2xl font-bold text-white">4</p>
                        </div>
                        <div className="p-2 rounded-lg bg-yellow-500/20">
                            <Bell className="w-6 h-6 text-yellow-400" />
                        </div>
                    </div>
                </div>
                <div className="glass-panel p-4 border-l-4 border-blue-500">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-400">Total Active</p>
                            <p className="text-2xl font-bold text-white">12</p>
                        </div>
                        <div className="p-2 rounded-lg bg-blue-500/20">
                            <AlertTriangle className="w-6 h-6 text-blue-400" />
                        </div>
                    </div>
                </div>
            </div>

            {/* Placeholder */}
            <div className="glass-panel p-12 flex flex-col items-center justify-center">
                <div className="p-4 rounded-full bg-yellow-500/20 mb-4">
                    <AlertTriangle className="w-12 h-12 text-yellow-400" />
                </div>
                <h2 className="text-xl font-semibold text-white mb-2">Alerts Management Coming Soon</h2>
                <p className="text-gray-400 text-center max-w-md">
                    This page will display all active alerts including DELAY_RISK_HIGH and ANOMALY_DETECTED events.
                    Filter by severity, date range, and alert type.
                </p>
                <p className="text-sm text-primary mt-4">Phase 5 Implementation</p>
            </div>
        </div>
    );
}
