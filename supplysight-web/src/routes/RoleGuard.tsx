import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/store';
import { ROUTES } from '@/lib/constants';
import type { UserRole } from '@/types';

interface RoleGuardProps {
    allowedRoles: UserRole[];
    fallback?: React.ReactNode;
}

/**
 * Role Guard component
 * Restricts access to routes based on user roles
 * Redirects to unauthorized page if user lacks required role
 */
export function RoleGuard({ allowedRoles, fallback }: RoleGuardProps) {
    const { hasAnyRole, isAuthenticated } = useAuth();
    const location = useLocation();

    // Should not reach here if not authenticated (ProtectedRoute should catch first)
    if (!isAuthenticated) {
        return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />;
    }

    // Check if user has any of the allowed roles
    if (!hasAnyRole(allowedRoles)) {
        // If custom fallback provided, render it
        if (fallback) {
            return <>{fallback}</>;
        }

        // Otherwise redirect to unauthorized page
        return <Navigate to={ROUTES.UNAUTHORIZED} replace />;
    }

    // User has required role - render child routes
    return <Outlet />;
}
