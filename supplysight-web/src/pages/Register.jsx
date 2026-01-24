import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { UserPlus, Mail, Lock, User, Briefcase, ArrowRight, Loader2 } from 'lucide-react';

export default function Register() {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        email: '',
        username: '',
        password: '',
        fullName: '',
        tenantName: ''
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            const response = await fetch('http://localhost:8081/api/v1/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData),
            });

            if (response.ok) {
                navigate('/login');
            } else {
                const data = await response.json().catch(() => ({}));
                setError(data.message || 'Registration failed');
            }
        } catch (err) {
            setError('Backend unavailable. Registration requires the backend to be running.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-background flex items-center justify-center p-4 relative overflow-hidden">
            {/* Background Ambience */}
            <div className="absolute top-[-20%] right-[-10%] w-[60%] h-[60%] bg-secondary/10 rounded-full blur-[120px]" />
            <div className="absolute bottom-[-20%] left-[-10%] w-[60%] h-[60%] bg-primary/10 rounded-full blur-[120px]" />

            <div className="w-full max-w-md glass-panel p-8 relative z-10 animate-in fade-in zoom-in duration-500">
                <div className="text-center mb-8">
                    <h1 className="text-3xl font-bold mb-2 bg-clip-text text-transparent bg-gradient-to-r from-secondary to-accent">
                        Create Account
                    </h1>
                    <p className="text-gray-400">Join SupplySight today</p>
                </div>

                <form onSubmit={handleRegister} className="space-y-4">
                    <div className="space-y-2">
                        <label className="text-sm font-medium text-gray-300">Full Name</label>
                        <div className="relative">
                            <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
                            <input
                                name="fullName"
                                required
                                className="input-field pl-10"
                                placeholder="John Doe"
                                value={formData.fullName}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="text-sm font-medium text-gray-300">Email</label>
                        <div className="relative">
                            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
                            <input
                                name="email"
                                type="email"
                                required
                                className="input-field pl-10"
                                placeholder="john@example.com"
                                value={formData.email}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="text-sm font-medium text-gray-300">Username</label>
                        <div className="relative">
                            <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
                            <input
                                name="username"
                                required
                                className="input-field pl-10"
                                placeholder="johndoe"
                                value={formData.username}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="text-sm font-medium text-gray-300">Password</label>
                        <div className="relative">
                            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
                            <input
                                name="password"
                                type="password"
                                required
                                className="input-field pl-10"
                                placeholder="••••••••"
                                value={formData.password}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="text-sm font-medium text-gray-300">Tenant Name</label>
                        <div className="relative">
                            <Briefcase className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
                            <input
                                name="tenantName"
                                className="input-field pl-10"
                                placeholder="My Company (Optional)"
                                value={formData.tenantName}
                                onChange={handleChange}
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
                        className="w-full btn-primary flex items-center justify-center gap-2 group bg-gradient-to-r from-secondary to-purple-600"
                    >
                        {loading ? (
                            <Loader2 className="w-5 h-5 animate-spin" />
                        ) : (
                            <>
                                Register
                                <UserPlus className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                            </>
                        )}
                    </button>
                </form>

                <div className="mt-6 text-center text-sm text-gray-400">
                    Already have an account?{' '}
                    <Link to="/login" className="text-secondary hover:text-white transition-colors">
                        Sign In
                    </Link>
                </div>
            </div>
        </div>
    );
}
