import { ShieldOff, ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/lib/constants';
import { useAuth } from '@/store';

/**
 * Unauthorized Page
 * Shown when user tries to access a route they don't have permission for
 */
export function UnauthorizedPage() {
    const { roles } = useAuth();

    return (
        <div className="min-h-screen bg-background flex items-center justify-center p-4">
            {/* Background Ambience */}
            <div className="fixed inset-0 overflow-hidden pointer-events-none -z-10">
                <div className="absolute top-[-20%] left-[-10%] w-[60%] h-[60%] bg-red-500/10 rounded-full blur-[120px]" />
                <div className="absolute bottom-[-20%] right-[-10%] w-[60%] h-[60%] bg-primary/10 rounded-full blur-[120px]" />
            </div>

            <div className="glass-panel p-8 max-w-md w-full text-center">
                <div className="p-4 rounded-full bg-red-500/20 w-fit mx-auto mb-6">
                    <ShieldOff className="w-12 h-12 text-red-400" />
                </div>

                <h1 className="text-2xl font-bold text-white mb-2">Access Denied</h1>
                <p className="text-gray-400 mb-6">
                    You don't have permission to access this page.
                    Your current role{roles.length > 1 ? 's' : ''}: {roles.join(', ') || 'None'}
                </p>

                <Link
                    to={ROUTES.DASHBOARD}
                    className="btn-primary inline-flex items-center gap-2"
                >
                    <ArrowLeft className="w-4 h-4" />
                    <span>Back to Dashboard</span>
                </Link>
            </div>
        </div>
    );
}
