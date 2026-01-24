import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Lock, Mail, ArrowRight, Loader2 } from 'lucide-react';

export default function Login() {
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            // Try real login first
            const response = await fetch('http://localhost:8081/api/v1/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password }),
            });

            if (response.ok) {
                const data = await response.json();
                localStorage.setItem('token', data.token);
                localStorage.setItem('user', JSON.stringify({ email }));
                navigate('/');
                return;
            }
        } catch (err) {
            console.warn("Backend unavailable, falling back to demo mode if credentials match demo");
        }

        // Demo Mode Fallback
        if (email === 'admin@demo.com' && password === 'admin123') {
            await new Promise(r => setTimeout(r, 1000)); // Fake delay
            localStorage.setItem('token', 'demo-token');
            localStorage.setItem('user', JSON.stringify({ email }));
            navigate('/');
        } else {
            setError('Invalid credentials (Try admin@demo.com / admin123)');
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-background flex items-center justify-center p-4 relative overflow-hidden">
            {/* Background Ambience */}
            <div className="absolute top-[-20%] left-[-10%] w-[60%] h-[60%] bg-primary/10 rounded-full blur-[120px]" />
            <div className="absolute bottom-[-20%] right-[-10%] w-[60%] h-[60%] bg-secondary/10 rounded-full blur-[120px]" />

            <div className="w-full max-w-md glass-panel p-8 relative z-10 animate-in fade-in zoom-in duration-500">
                <div className="text-center mb-8">
                    <h1 className="text-3xl font-bold mb-2 bg-clip-text text-transparent bg-gradient-to-r from-primary to-accent">
                        Welcome Back
                    </h1>
                    <p className="text-gray-400">Sign in to access your supply chain</p>
                </div>

                <form onSubmit={handleLogin} className="space-y-6">
                    <div className="space-y-2">
                        <label className="text-sm font-medium text-gray-300">Email</label>
                        <div className="relative">
                            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
                            <input
                                type="email"
                                required
                                className="input-field pl-10"
                                placeholder="admin@demo.com"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="text-sm font-medium text-gray-300">Password</label>
                        <div className="relative">
                            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
                            <input
                                type="password"
                                required
                                className="input-field pl-10"
                                placeholder="••••••••"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </div>
                    </div>

                    {error && (
                        <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-200 text-sm">
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full btn-primary flex items-center justify-center gap-2 group"
                    >
                        {loading ? (
                            <Loader2 className="w-5 h-5 animate-spin" />
                        ) : (
                            <>
                                Sign In
                                <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                            </>
                        )}

                    </button>
                </form>

                <div className="mt-6 text-center text-sm text-gray-400">
                    Don't have an account?{' '}
                    <Link to="/register" className="text-secondary hover:text-white transition-colors">
                        Create Account
                    </Link>
                </div>
            </div>
        </div >
    );
}
