import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Layouts
import { AuthLayout, AppShell } from '@/layouts';

// Route guards
import { ProtectedRoute, RoleGuard } from '@/routes';

// Feature pages
import { LoginPage } from '@/features/auth';
import { DashboardPage } from '@/features/dashboard';
import { ShipmentsPage, ShipmentDetailPage } from '@/features/shipments';
import { AlertsPage, AlertsErrorBoundary } from '@/features/alerts';
import { SettingsPage } from '@/features/settings';
import { UnauthorizedPage } from '@/features/common';

// Store
import { useAuthStore } from '@/store';

// Constants
import { ROUTES } from '@/lib/constants';

// Create React Query client
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 1000 * 60 * 5, // 5 minutes
            retry: 1,
            refetchOnWindowFocus: false,
        },
    },
});

/**
 * Auth Initializer Component
 * Initializes auth state on app load
 */
function AuthInitializer({ children }: { children: React.ReactNode }) {
    const initialize = useAuthStore((state) => state.initialize);
    const isInitialized = useAuthStore((state) => state.isInitialized);

    useEffect(() => {
        initialize();
    }, [initialize]);

    // Show nothing while initializing (ProtectedRoute will show loading)
    if (!isInitialized) {
        return null;
    }

    return <>{children}</>;
}

/**
 * Main App Component
 */
function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>
                <AuthInitializer>
                    <Routes>
                        {/* Public routes - Auth layout */}
                        <Route element={<AuthLayout />}>
                            <Route path={ROUTES.LOGIN} element={<LoginPage />} />
                        </Route>

                        {/* Protected routes - App shell layout */}
                        <Route element={<ProtectedRoute />}>
                            <Route element={<AppShell />}>
                                {/* Dashboard - All authenticated users */}
                                <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />

                                {/* Shipments - All authenticated users */}
                                <Route path={ROUTES.SHIPMENTS} element={<ShipmentsPage />} />
                                <Route path={ROUTES.SHIPMENT_DETAIL} element={<ShipmentDetailPage />} />

                                {/* Alerts - OPS_USER and ADMIN only */}
                                <Route element={<RoleGuard allowedRoles={['ADMIN', 'OPS_USER']} />}>
                                    <Route
                                        path={ROUTES.ALERTS}
                                        element={
                                            <AlertsErrorBoundary>
                                                <AlertsPage />
                                            </AlertsErrorBoundary>
                                        }
                                    />
                                </Route>

                                {/* Settings - ADMIN only */}
                                <Route element={<RoleGuard allowedRoles={['ADMIN']} />}>
                                    <Route path={ROUTES.SETTINGS} element={<SettingsPage />} />
                                </Route>
                            </Route>
                        </Route>

                        {/* Unauthorized page - accessible by all */}
                        <Route path={ROUTES.UNAUTHORIZED} element={<UnauthorizedPage />} />

                        {/* Catch-all redirect */}
                        <Route path="*" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
                    </Routes>
                </AuthInitializer>
            </BrowserRouter>
        </QueryClientProvider>
    );
}

export default App;
