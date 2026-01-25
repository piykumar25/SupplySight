/**
 * User roles in the SupplySight system
 */
export type UserRole = 'ADMIN' | 'OPS_USER' | 'VIEWER';

/**
 * User entity from the Identity Service
 */
export interface User {
    id: string;
    tenantId: string;
    tenantCode: string;
    tenantName?: string;
    email: string;
    username: string;
    firstName: string;
    lastName: string;
    fullName: string;
    roles: UserRole[];
}

/**
 * Login request payload
 */
export interface LoginRequest {
    email: string;
    password: string;
}

/**
 * Login response from Identity Service
 */
export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: 'Bearer';
    expiresIn: number;
    user: User;
}

/**
 * Token refresh response
 */
export interface RefreshResponse {
    accessToken: string;
    tokenType: 'Bearer';
    expiresIn: number;
}

/**
 * Logout request payload
 */
export interface LogoutRequest {
    refreshToken: string;
}

/**
 * Decoded JWT payload
 */
export interface JwtPayload {
    sub: string;
    tenantId: string;
    roles: UserRole[];
    iat: number;
    exp: number;
}
