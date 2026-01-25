import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/store';
import { ROUTES } from '@/lib/constants';

/**
 * Loading spinner component for auth initialization
 */
function AuthLoading() {
    return (
        <div className="min-h-screen bg-background flex items-center justify-center">
            <div className="flex flex-col items-center gap-4">
                <div className="w-12 h-12 border-4 border-primary/30 border-t-primary rounded-full animate-spin" />
                <p className="text-gray-400 animate-pulse">Loading...</p>
            </div>
        </div>
    );
}

/**
 * Protected Route component
 * Redirects to login if user is not authenticated
 * Shows loading state during auth initialization
 */
export function ProtectedRoute() {
    const { isAuthenticated, isInitialized, isLoading } = useAuth();
    const location = useLocation();

    // Show loading while initializing auth state
    if (!isInitialized || isLoading) {
        return <AuthLoading />;
    }

    // Redirect to login if not authenticated
    if (!isAuthenticated) {
        // Save the attempted URL for post-login redirect
        return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />;
    }

    // Render child routes
    return <Outlet />;
}
