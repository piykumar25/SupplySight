import { create } from 'zustand';
import { jwtDecode } from 'jwt-decode';
import { authApi, registerAuthFunctions } from '@/api';
import { storage, isTokenExpired } from '@/lib/utils';
import { STORAGE_KEYS, ROUTES } from '@/lib/constants';
import type { User, UserRole, JwtPayload, LoginRequest } from '@/types';

/**
 * Auth store state interface
 */
interface AuthState {
    // User data
    user: User | null;
    accessToken: string | null;
    refreshToken: string | null;

    // Tenant context
    tenantId: string | null;
    tenantCode: string | null;
    tenantName: string | null;

    // Roles for RBAC
    roles: UserRole[];

    // Status flags
    isAuthenticated: boolean;
    isLoading: boolean;
    isInitialized: boolean;
    error: string | null;
}

/**
 * Auth store actions interface
 */
interface AuthActions {
    // Actions
    login: (credentials: LoginRequest) => Promise<void>;
    logout: () => Promise<void>;
    refreshAccessToken: () => Promise<string | null>;
    initialize: () => Promise<void>;
    clearError: () => void;

    // Role checks
    hasRole: (role: UserRole) => boolean;
    hasAnyRole: (roles: UserRole[]) => boolean;
}

type AuthStore = AuthState & AuthActions;

/**
 * Initial state
 */
const initialState: AuthState = {
    user: null,
    accessToken: null,
    refreshToken: null,
    tenantId: null,
    tenantCode: null,
    tenantName: null,
    roles: [],
    isAuthenticated: false,
    isLoading: false,
    isInitialized: false,
    error: null,
};

/**
 * Auth store using Zustand
 * Manages authentication state, tokens, and user context
 */
export const useAuthStore = create<AuthStore>((set, get) => {
    // Create the store
    const store: AuthStore = {
        ...initialState,

        /**
         * Login with email and password
         */
        login: async (credentials: LoginRequest) => {
            set({ isLoading: true, error: null });

            try {
                const response = await authApi.login(credentials);

                // Store refresh token in localStorage
                storage.set(STORAGE_KEYS.REFRESH_TOKEN, response.refreshToken);
                storage.set(STORAGE_KEYS.USER, response.user);

                // Update state with tokens and user
                set({
                    accessToken: response.accessToken,
                    refreshToken: response.refreshToken,
                    user: response.user,
                    tenantId: response.user.tenantId,
                    tenantCode: response.user.tenantCode,
                    tenantName: response.user.tenantName || null,
                    roles: response.user.roles,
                    isAuthenticated: true,
                    isLoading: false,
                    error: null,
                });
            } catch (error: unknown) {
                const errorMessage =
                    (error as { error?: { message?: string } })?.error?.message ||
                    'Login failed. Please check your credentials.';

                set({
                    isLoading: false,
                    error: errorMessage,
                    isAuthenticated: false,
                });
                throw error;
            }
        },

        /**
         * Logout and cleanup
         */
        logout: async () => {
            const { refreshToken } = get();

            try {
                if (refreshToken) {
                    await authApi.logout(refreshToken);
                }
            } catch {
                // Ignore logout errors - we'll clear local state anyway
                console.warn('Logout API call failed, clearing local state');
            }

            // Clear storage
            storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
            storage.remove(STORAGE_KEYS.USER);

            // Reset state
            set({
                ...initialState,
                isInitialized: true,
            });

            // Redirect to login
            window.location.href = ROUTES.LOGIN;
        },

        /**
         * Refresh access token using stored refresh token
         */
        refreshAccessToken: async () => {
            const { refreshToken } = get();

            if (!refreshToken) {
                return null;
            }

            try {
                const response = await authApi.refreshToken(refreshToken);

                set({ accessToken: response.accessToken });

                return response.accessToken;
            } catch (error) {
                // Refresh failed - logout user
                console.error('Token refresh failed:', error);
                get().logout();
                return null;
            }
        },

        /**
         * Initialize auth state from stored tokens
         * Called on app startup
         */
        initialize: async () => {
            set({ isLoading: true });

            try {
                // Get stored refresh token
                const storedRefreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN);
                const storedUser = storage.get<User>(STORAGE_KEYS.USER);

                if (!storedRefreshToken) {
                    set({ isInitialized: true, isLoading: false });
                    return;
                }

                // Set refresh token first
                set({ refreshToken: storedRefreshToken });

                // Try to refresh access token
                const response = await authApi.refreshToken(storedRefreshToken);

                // Decode token to get expiry
                const decoded = jwtDecode<JwtPayload>(response.accessToken);

                if (isTokenExpired(decoded.exp)) {
                    throw new Error('Token expired');
                }

                // If we have stored user, use it; otherwise fetch fresh
                let user = storedUser;
                if (!user) {
                    // Set token first so the API call is authenticated
                    set({ accessToken: response.accessToken });
                    user = await authApi.getCurrentUser();
                    storage.set(STORAGE_KEYS.USER, user);
                }

                set({
                    accessToken: response.accessToken,
                    refreshToken: storedRefreshToken,
                    user,
                    tenantId: user.tenantId,
                    tenantCode: user.tenantCode,
                    tenantName: user.tenantName || null,
                    roles: user.roles,
                    isAuthenticated: true,
                    isLoading: false,
                    isInitialized: true,
                    error: null,
                });
            } catch (error) {
                console.error('Auth initialization failed:', error);

                // Clear invalid tokens
                storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
                storage.remove(STORAGE_KEYS.USER);

                set({
                    ...initialState,
                    isInitialized: true,
                });
            }
        },

        /**
         * Clear error state
         */
        clearError: () => set({ error: null }),

        /**
         * Check if user has a specific role
         */
        hasRole: (role: UserRole) => {
            const { roles } = get();
            return roles.includes(role);
        },

        /**
         * Check if user has any of the specified roles
         */
        hasAnyRole: (allowedRoles: UserRole[]) => {
            const { roles } = get();
            return roles.some(role => allowedRoles.includes(role));
        },
    };

    // Register auth functions with axios interceptors
    registerAuthFunctions(
        () => store.accessToken,
        store.refreshAccessToken
    );

    return store;
});

/**
 * Hook to get auth selectors
 */
export const useAuth = () => {
    const store = useAuthStore();
    return {
        user: store.user,
        isAuthenticated: store.isAuthenticated,
        isLoading: store.isLoading,
        isInitialized: store.isInitialized,
        error: store.error,
        tenantId: store.tenantId,
        tenantCode: store.tenantCode,
        tenantName: store.tenantName,
        roles: store.roles,
        hasRole: store.hasRole,
        hasAnyRole: store.hasAnyRole,
        login: store.login,
        logout: store.logout,
        clearError: store.clearError,
    };
};
