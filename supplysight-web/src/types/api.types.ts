/**
 * Standard API response wrapper from backend services
 */
export interface ApiResponse<T> {
    success: true;
    data: T;
    timestamp: string;
    correlationId?: string;
}

/**
 * Field-level validation errors
 */
export interface FieldErrors {
    [field: string]: string;
}

/**
 * Standard API error response
 */
export interface ApiError {
    success: false;
    error: {
        code: string;
        message: string;
        fieldErrors?: FieldErrors;
    };
    timestamp: string;
    correlationId?: string;
}

/**
 * Union type for API responses
 */
export type ApiResult<T> = ApiResponse<T> | ApiError;

/**
 * Pagination parameters for list endpoints
 */
export interface PaginationParams {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: 'asc' | 'desc';
}

/**
 * Paginated response wrapper
 */
export interface PaginatedResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}

/**
 * Error codes from the backend
 */
export type ErrorCode =
    | 'VALIDATION_FAILED'
    | 'UNAUTHORIZED'
    | 'FORBIDDEN'
    | 'RESOURCE_NOT_FOUND'
    | 'DUPLICATE_RESOURCE'
    | 'INTERNAL_ERROR';
