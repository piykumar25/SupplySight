import { Outlet } from 'react-router-dom';
import { Truck } from 'lucide-react';

/**
 * Auth Layout
 * Clean layout for login and registration pages with branding
 */
export function AuthLayout() {
    return (
        <div className="min-h-screen bg-background flex flex-col">
            {/* Background Ambience */}
            <div className="fixed inset-0 overflow-hidden pointer-events-none -z-10">
                <div className="absolute top-[-20%] left-[-10%] w-[60%] h-[60%] bg-primary/10 rounded-full blur-[120px]" />
                <div className="absolute bottom-[-20%] right-[-10%] w-[60%] h-[60%] bg-secondary/10 rounded-full blur-[120px]" />
            </div>

            {/* Header with logo */}
            <header className="h-16 flex items-center justify-center border-b border-white/5">
                <div className="flex items-center gap-3">
                    <div className="p-2 bg-gradient-to-br from-primary to-blue-600 rounded-lg shadow-lg shadow-primary/20">
                        <Truck className="w-6 h-6 text-white" />
                    </div>
                    <span className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-white/70">
                        SupplySight
                    </span>
                </div>
            </header>

            {/* Main content */}
            <main className="flex-1 flex items-center justify-center p-4">
                <Outlet />
            </main>

            {/* Footer */}
            <footer className="h-12 flex items-center justify-center text-sm text-gray-500">
                <p>© 2026 SupplySight. All rights reserved.</p>
            </footer>
        </div>
    );
}
