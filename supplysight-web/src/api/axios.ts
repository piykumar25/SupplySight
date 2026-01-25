import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URLS, HEADERS, ROUTES, STORAGE_KEYS } from '@/lib/constants';
import { generateCorrelationId, storage } from '@/lib/utils';
import type { ApiError, ApiResponse } from '@/types';

// Token getter - will be set by auth store
let getAccessToken: (() => string | null) | null = null;
let refreshTokenFn: (() => Promise<string | null>) | null = null;

/**
 * Register the auth store's token getter and refresh function
 * This is called during store initialization to avoid circular dependencies
 */
export function registerAuthFunctions(
    tokenGetter: () => string | null,
    refreshFn: () => Promise<string | null>
) {
    getAccessToken = tokenGetter;
    refreshTokenFn = refreshFn;
}

/**
 * Create an Axios instance with interceptors for a specific service
 */
function createApiClient(baseURL: string): AxiosInstance {
    const client = axios.create({
        baseURL,
        timeout: 30000,
        headers: {
            'Content-Type': 'application/json',
        },
    });

    // Request interceptor
    client.interceptors.request.use(
        (config: InternalAxiosRequestConfig) => {
            // Add correlation ID for request tracing
            config.headers[HEADERS.CORRELATION_ID] = generateCorrelationId();

            // Add JWT if available
            const token = getAccessToken?.();
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }

            return config;
        },
        (error) => Promise.reject(error)
    );

    // Response interceptor
    client.interceptors.response.use(
        (response) => response,
        async (error: AxiosError<ApiError>) => {
            const originalRequest = error.config;

            // Handle 401 Unauthorized - attempt token refresh
            if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
                originalRequest._retry = true;

                try {
                    const newToken = await refreshTokenFn?.();
                    if (newToken && originalRequest.headers) {
                        originalRequest.headers.Authorization = `Bearer ${newToken}`;
                        return client(originalRequest);
                    }
                } catch (refreshError) {
                    // Refresh failed - clear auth and redirect to login
                    storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
                    storage.remove(STORAGE_KEYS.USER);
                    window.location.href = ROUTES.LOGIN;
                    return Promise.reject(refreshError);
                }
            }

            // Normalize error response
            const normalizedError: ApiError = error.response?.data || {
                success: false,
                error: {
                    code: 'NETWORK_ERROR',
                    message: error.message || 'An unexpected error occurred',
                },
                timestamp: new Date().toISOString(),
            };

            return Promise.reject(normalizedError);
        }
    );

    return client;
}

// Extend AxiosRequestConfig to include retry flag
declare module 'axios' {
    export interface InternalAxiosRequestConfig {
        _retry?: boolean;
    }
}

// API clients for each service
export const identityApi = createApiClient(API_BASE_URLS.IDENTITY);
export const trackingApi = createApiClient(API_BASE_URLS.TRACKING);
export const visibilityApi = createApiClient(API_BASE_URLS.VISIBILITY);
export const predictionApi = createApiClient(API_BASE_URLS.PREDICTION);

/**
 * Type-safe API response handler
 */
export async function apiRequest<T>(
    request: Promise<{ data: ApiResponse<T> }>
): Promise<T> {
    const response = await request;
    return response.data.data;
}
