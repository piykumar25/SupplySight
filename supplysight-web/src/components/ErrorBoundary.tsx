import React, { Component, ErrorInfo, ReactNode } from 'react';
import { telemetry } from '../lib/telemetry';

interface Props {
    children: ReactNode;
    fallback?: ReactNode;
}

interface State {
    hasError: boolean;
    error?: Error;
}

/**
 * Error Boundary component with telemetry integration.
 * Catches React errors and reports them to the telemetry system.
 */
export class ErrorBoundary extends Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
        telemetry.trackError('react_error_boundary', error, {
            componentStack: errorInfo.componentStack?.substring(0, 500) || '',
        });

        // Log in development
        if (import.meta.env.DEV) {
            console.error('ErrorBoundary caught:', error, errorInfo);
        }
    }

    handleRetry = (): void => {
        this.setState({ hasError: false, error: undefined });
    };

    render(): ReactNode {
        if (this.state.hasError) {
            if (this.props.fallback) {
                return this.props.fallback;
            }

            return (
                <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
                    <div className="max-w-md w-full p-6 bg-white dark:bg-gray-800 rounded-lg shadow-lg">
                        <div className="flex items-center gap-3 mb-4">
                            <div className="w-10 h-10 rounded-full bg-red-100 dark:bg-red-900 flex items-center justify-center">
                                <svg
                                    className="w-6 h-6 text-red-600 dark:text-red-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                                    />
                                </svg>
                            </div>
                            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                                Something went wrong
                            </h2>
                        </div>

                        <p className="text-gray-600 dark:text-gray-300 mb-4">
                            We encountered an unexpected error. Our team has been notified.
                        </p>

                        {import.meta.env.DEV && this.state.error && (
                            <pre className="bg-gray-100 dark:bg-gray-700 p-3 rounded text-xs overflow-auto mb-4 text-red-600 dark:text-red-400">
                                {this.state.error.message}
                            </pre>
                        )}

                        <div className="flex gap-3">
                            <button
                                onClick={this.handleRetry}
                                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
                            >
                                Try Again
                            </button>
                            <button
                                onClick={() => window.location.reload()}
                                className="px-4 py-2 bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors"
                            >
                                Reload Page
                            </button>
                        </div>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
