import { useState } from 'react';
import { Outlet, Link, useLocation } from 'react-router-dom';
import {
    Truck,
    LogOut,
    LayoutDashboard,
    Package,
    AlertTriangle,
    Settings,
    ChevronLeft,
    ChevronRight,
    Menu,
    X,
    User,
    Building2,
} from 'lucide-react';
import { useAuth } from '@/store';
import { ROUTES } from '@/lib/constants';
import type { UserRole } from '@/types';

/**
 * Navigation item configuration
 */
interface NavItem {
    path: string;
    label: string;
    icon: React.ElementType;
    roles?: UserRole[]; // If undefined, all roles can access
}

/**
 * Navigation configuration
 */
const navItems: NavItem[] = [
    {
        path: ROUTES.DASHBOARD,
        label: 'Dashboard',
        icon: LayoutDashboard,
    },
    {
        path: ROUTES.SHIPMENTS,
        label: 'Shipments',
        icon: Package,
    },
    {
        path: ROUTES.ALERTS,
        label: 'Alerts',
        icon: AlertTriangle,
        roles: ['ADMIN', 'OPS_USER'],
    },
    {
        path: ROUTES.SETTINGS,
        label: 'Settings',
        icon: Settings,
        roles: ['ADMIN'],
    },
];

/**
 * Role badge colors
 */
const roleBadgeColors: Record<UserRole, string> = {
    ADMIN: 'bg-purple-500/20 text-purple-300 border-purple-500/30',
    OPS_USER: 'bg-blue-500/20 text-blue-300 border-blue-500/30',
    VIEWER: 'bg-gray-500/20 text-gray-300 border-gray-500/30',
};

/**
 * AppShell Layout
 * Main application layout with sidebar, header, and content area
 */
export function AppShell() {
    const { user, tenantName, tenantCode, roles, logout, hasAnyRole } = useAuth();
    const location = useLocation();
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    // Filter nav items based on user roles
    const filteredNavItems = navItems.filter(
        (item) => !item.roles || hasAnyRole(item.roles)
    );

    // Get primary role for badge display
    const primaryRole = roles[0] || 'VIEWER';

    const handleLogout = async () => {
        await logout();
    };

    return (
        <div className="min-h-screen bg-background text-white flex">
            {/* Background gradients */}
            <div className="fixed inset-0 overflow-hidden -z-10 pointer-events-none">
                <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-primary/15 rounded-full blur-[100px]" />
                <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-secondary/15 rounded-full blur-[100px]" />
            </div>

            {/* Mobile menu overlay */}
            {mobileMenuOpen && (
                <div
                    className="fixed inset-0 bg-black/60 z-40 lg:hidden"
                    onClick={() => setMobileMenuOpen(false)}
                />
            )}

            {/* Sidebar */}
            <aside
                className={`
          fixed lg:static inset-y-0 left-0 z-50
          flex flex-col
          bg-surface/80 backdrop-blur-xl
          border-r border-white/10
          transition-all duration-300 ease-in-out
          ${sidebarCollapsed ? 'w-20' : 'w-64'}
          ${mobileMenuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
        `}
            >
                {/* Sidebar Header */}
                <div className="h-16 flex items-center justify-between px-4 border-b border-white/10">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-gradient-to-br from-primary to-blue-600 rounded-lg shadow-lg shadow-primary/20 flex-shrink-0">
                            <Truck className="w-5 h-5 text-white" />
                        </div>
                        {!sidebarCollapsed && (
                            <span className="text-lg font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-white/70">
                                SupplySight
                            </span>
                        )}
                    </div>

                    {/* Collapse button (desktop) */}
                    <button
                        onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
                        className="hidden lg:flex p-1.5 rounded-lg hover:bg-white/10 transition-colors text-gray-400 hover:text-white"
                    >
                        {sidebarCollapsed ? (
                            <ChevronRight className="w-4 h-4" />
                        ) : (
                            <ChevronLeft className="w-4 h-4" />
                        )}
                    </button>

                    {/* Close button (mobile) */}
                    <button
                        onClick={() => setMobileMenuOpen(false)}
                        className="lg:hidden p-1.5 rounded-lg hover:bg-white/10 transition-colors text-gray-400 hover:text-white"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Navigation */}
                <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
                    {filteredNavItems.map((item) => {
                        const isActive = location.pathname === item.path;
                        const Icon = item.icon;

                        return (
                            <Link
                                key={item.path}
                                to={item.path}
                                onClick={() => setMobileMenuOpen(false)}
                                className={`
                  flex items-center gap-3 px-3 py-2.5 rounded-lg
                  transition-all duration-200
                  ${isActive
                                        ? 'bg-primary/20 text-white shadow-lg shadow-primary/10'
                                        : 'text-gray-400 hover:bg-white/10 hover:text-white'
                                    }
                `}
                            >
                                <Icon className={`w-5 h-5 flex-shrink-0 ${isActive ? 'text-primary' : ''}`} />
                                {!sidebarCollapsed && (
                                    <span className="font-medium">{item.label}</span>
                                )}
                            </Link>
                        );
                    })}
                </nav>

                {/* Sidebar Footer - User section */}
                <div className="p-3 border-t border-white/10">
                    {!sidebarCollapsed ? (
                        <div className="p-3 rounded-lg bg-white/5">
                            <div className="flex items-center gap-3 mb-3">
                                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-secondary flex items-center justify-center flex-shrink-0">
                                    <User className="w-5 h-5 text-white" />
                                </div>
                                <div className="flex-1 min-w-0">
                                    <p className="text-sm font-medium text-white truncate">
                                        {user?.fullName || 'User'}
                                    </p>
                                    <p className="text-xs text-gray-400 truncate">{user?.email}</p>
                                </div>
                            </div>

                            <div className="flex items-center justify-between">
                                <span className={`text-xs px-2 py-0.5 rounded border ${roleBadgeColors[primaryRole]}`}>
                                    {primaryRole.replace('_', ' ')}
                                </span>
                                <button
                                    onClick={handleLogout}
                                    className="p-1.5 rounded-lg hover:bg-white/10 transition-colors text-gray-400 hover:text-red-400"
                                    title="Logout"
                                >
                                    <LogOut className="w-4 h-4" />
                                </button>
                            </div>
                        </div>
                    ) : (
                        <button
                            onClick={handleLogout}
                            className="w-full p-2.5 rounded-lg hover:bg-white/10 transition-colors text-gray-400 hover:text-red-400 flex justify-center"
                            title="Logout"
                        >
                            <LogOut className="w-5 h-5" />
                        </button>
                    )}
                </div>
            </aside>

            {/* Main content area */}
            <div className="flex-1 flex flex-col min-w-0">
                {/* Top bar */}
                <header className="h-16 bg-surface/50 backdrop-blur-lg border-b border-white/10 flex items-center justify-between px-4 lg:px-6 sticky top-0 z-30">
                    {/* Mobile menu button */}
                    <button
                        onClick={() => setMobileMenuOpen(true)}
                        className="lg:hidden p-2 rounded-lg hover:bg-white/10 transition-colors text-gray-400 hover:text-white"
                    >
                        <Menu className="w-5 h-5" />
                    </button>

                    {/* Tenant context */}
                    <div className="hidden lg:flex items-center gap-3">
                        <div className="p-2 rounded-lg bg-white/5">
                            <Building2 className="w-4 h-4 text-primary" />
                        </div>
                        <div>
                            <p className="text-sm font-medium text-white">
                                {tenantName || tenantCode || 'Organization'}
                            </p>
                            {tenantCode && tenantName && (
                                <p className="text-xs text-gray-500 font-mono">{tenantCode}</p>
                            )}
                        </div>
                    </div>

                    {/* Right side actions */}
                    <div className="flex items-center gap-4">
                        {/* Role badge (mobile) */}
                        <span className={`lg:hidden text-xs px-2 py-0.5 rounded border ${roleBadgeColors[primaryRole]}`}>
                            {primaryRole.replace('_', ' ')}
                        </span>

                        {/* User info (desktop) */}
                        <div className="hidden lg:flex items-center gap-3">
                            <div className="text-right">
                                <p className="text-sm font-medium text-white">
                                    {user?.fullName || 'User'}
                                </p>
                                <p className="text-xs text-gray-400">{user?.email}</p>
                            </div>
                            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-secondary flex items-center justify-center">
                                <User className="w-5 h-5 text-white" />
                            </div>
                        </div>
                    </div>
                </header>

                {/* Page content */}
                <main className="flex-1 p-4 lg:p-6 overflow-auto">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
