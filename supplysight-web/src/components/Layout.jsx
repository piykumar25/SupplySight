import { Outlet, useNavigate } from 'react-router-dom';
import { Truck, LogOut } from 'lucide-react';

export default function Layout() {
    const navigate = useNavigate();
    const user = JSON.parse(localStorage.getItem('user') || '{}');

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        navigate('/login');
    };

    return (
        <div className="min-h-screen bg-background text-white relative overflow-hidden">
            {/* Background gradients */}
            <div className="fixed top-0 left-0 w-full h-full overflow-hidden -z-10 pointer-events-none">
                <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-primary/20 rounded-full blur-[100px]" />
                <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-secondary/20 rounded-full blur-[100px]" />
            </div>

            {/* Navbar */}
            <nav className="h-16 border-b border-white/10 bg-surface/50 backdrop-blur-lg sticky top-0 z-50">
                <div className="container mx-auto h-full px-4 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-gradient-to-br from-primary to-blue-600 rounded-lg shadow-lg shadow-primary/20">
                            <Truck className="w-6 h-6 text-white" />
                        </div>
                        <span className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-white/70">
                            SupplySight
                        </span>
                    </div>

                    <div className="flex items-center gap-4">
                        <div className="text-sm text-gray-400">
                            Logged in as <span className="text-white font-medium">{user.email || 'Guest'}</span>
                        </div>
                        <button
                            onClick={handleLogout}
                            className="p-2 hover:bg-white/10 rounded-lg transition-colors text-gray-400 hover:text-white"
                        >
                            <LogOut className="w-5 h-5" />
                        </button>
                    </div>
                </div>
            </nav>

            {/* Content */}
            <main className="container mx-auto p-4 md:p-6">
                <Outlet />
            </main>
        </div>
    );
}
