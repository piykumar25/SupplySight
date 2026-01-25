import { identityApi, apiRequest } from './axios';
import type {
    AuthResponse,
    RefreshResponse,
    LoginRequest,
    LogoutRequest,
    User
} from '@/types';
import type { ApiResponse } from '@/types';

/**
 * Identity Service API client
 * Handles authentication, user management, and tenant operations
 */
export const authApi = {
    /**
     * Authenticate user with email and password
     */
    login: async (credentials: LoginRequest): Promise<AuthResponse> => {
        const response = await identityApi.post<ApiResponse<AuthResponse>>(
            '/login',
            credentials
        );
        return response.data.data;
    },

    /**
     * Refresh access token using refresh token
     */
    refreshToken: async (refreshToken: string): Promise<RefreshResponse> => {
        const response = await identityApi.post<ApiResponse<RefreshResponse>>(
            '/refresh',
            { refreshToken }
        );
        return response.data.data;
    },

    /**
     * Logout user and invalidate tokens
     */
    logout: async (refreshToken: string): Promise<void> => {
        await identityApi.post('/logout', { refreshToken } as LogoutRequest);
    },

    /**
     * Get current user profile
     */
    getCurrentUser: async (): Promise<User> => {
        return apiRequest<User>(identityApi.get('/me'));
    },
};
