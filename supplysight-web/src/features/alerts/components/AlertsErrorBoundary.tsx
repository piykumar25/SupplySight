/**
 * Error Boundary for Alerts Feature
 * 
 * Catches React errors in the alerts feature and displays a fallback UI
 */

import { Component, ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

/**
 * Error Boundary Component
 */
export class AlertsErrorBoundary extends Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
        console.error('Alerts Error Boundary caught error:', error, errorInfo);
    }

    handleReset = () => {
        this.setState({ hasError: false, error: null });
        window.location.reload();
    };

    render() {
        if (this.state.hasError) {
            return (
                <div className="min-h-[400px] flex items-center justify-center p-8">
                    <div className="max-w-md w-full bg-surface/50 border border-red-500/30 rounded-xl p-8 text-center">
                        <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-500/20 mb-4">
                            <AlertTriangle className="w-8 h-8 text-red-400" />
                        </div>

                        <h2 className="text-xl font-semibold text-white mb-2">
                            Something went wrong
                        </h2>

                        <p className="text-gray-400 text-sm mb-6">
                            An error occurred while loading the alerts feature. Please try refreshing the page.
                        </p>

                        {this.state.error && (
                            <details className="mb-6 text-left">
                                <summary className="text-xs text-gray-500 cursor-pointer hover:text-gray-400 mb-2">
                                    Error details
                                </summary>
                                <pre className="text-xs text-red-400 bg-black/20 rounded-lg p-3 overflow-x-auto">
                                    {this.state.error.message}
                                </pre>
                            </details>
                        )}

                        <button
                            onClick={this.handleReset}
                            className="inline-flex items-center gap-2 px-4 py-2 bg-red-500/20 hover:bg-red-500/30 text-red-400 rounded-lg transition-colors"
                        >
                            <RefreshCw className="w-4 h-4" />
                            <span>Reload Page</span>
                        </button>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}
