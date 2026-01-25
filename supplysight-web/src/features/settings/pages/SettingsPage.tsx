import { Settings as SettingsIcon, User, Building2, Shield, Bell } from 'lucide-react';
import { useAuth } from '@/store';

/**
 * Settings Page
 * Admin-only settings management
 */
export function SettingsPage() {
    const { user, tenantName, tenantCode } = useAuth();

    return (
        <div className="space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-2xl font-bold text-white">Settings</h1>
                <p className="text-gray-400">Manage your organization and preferences</p>
            </div>

            {/* Settings sections */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Organization */}
                <div className="glass-panel p-6">
                    <div className="flex items-center gap-3 mb-4">
                        <div className="p-2 rounded-lg bg-primary/20">
                            <Building2 className="w-5 h-5 text-primary" />
                        </div>
                        <h2 className="text-lg font-semibold text-white">Organization</h2>
                    </div>
                    <div className="space-y-3">
                        <div className="flex justify-between">
                            <span className="text-gray-400">Tenant Name</span>
                            <span className="text-white">{tenantName || 'Not set'}</span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-gray-400">Tenant Code</span>
                            <span className="text-white font-mono">{tenantCode || 'Not set'}</span>
                        </div>
                    </div>
                </div>

                {/* Profile */}
                <div className="glass-panel p-6">
                    <div className="flex items-center gap-3 mb-4">
                        <div className="p-2 rounded-lg bg-primary/20">
                            <User className="w-5 h-5 text-primary" />
                        </div>
                        <h2 className="text-lg font-semibold text-white">Profile</h2>
                    </div>
                    <div className="space-y-3">
                        <div className="flex justify-between">
                            <span className="text-gray-400">Name</span>
                            <span className="text-white">{user?.fullName || 'Not set'}</span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-gray-400">Email</span>
                            <span className="text-white">{user?.email || 'Not set'}</span>
                        </div>
                    </div>
                </div>

                {/* Security */}
                <div className="glass-panel p-6">
                    <div className="flex items-center gap-3 mb-4">
                        <div className="p-2 rounded-lg bg-primary/20">
                            <Shield className="w-5 h-5 text-primary" />
                        </div>
                        <h2 className="text-lg font-semibold text-white">Security</h2>
                    </div>
                    <p className="text-gray-500 text-sm">
                        User management, password policies, and role assignments will be available here.
                    </p>
                </div>

                {/* Notifications */}
                <div className="glass-panel p-6">
                    <div className="flex items-center gap-3 mb-4">
                        <div className="p-2 rounded-lg bg-primary/20">
                            <Bell className="w-5 h-5 text-primary" />
                        </div>
                        <h2 className="text-lg font-semibold text-white">Notifications</h2>
                    </div>
                    <p className="text-gray-500 text-sm">
                        Alert thresholds and notification preferences will be configurable here.
                    </p>
                </div>
            </div>

            {/* Admin notice */}
            <div className="glass-panel p-4 border-l-4 border-purple-500">
                <div className="flex items-center gap-2">
                    <SettingsIcon className="w-5 h-5 text-purple-400" />
                    <p className="text-sm text-gray-300">
                        <strong className="text-white">Admin Only:</strong> This page is only accessible to users with the ADMIN role.
                    </p>
                </div>
            </div>
        </div>
    );
}
