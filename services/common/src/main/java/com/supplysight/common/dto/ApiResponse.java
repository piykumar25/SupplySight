package com.supplysight.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Standard API response wrapper for consistent response structure across all services.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorDetails error,
    Instant timestamp,
    String correlationId
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return new ApiResponse<>(true, data, null, Instant.now(), correlationId);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetails(code, message, null), Instant.now(), null);
    }

    public static <T> ApiResponse<T> error(String code, String message, String correlationId) {
        return new ApiResponse<>(false, null, new ErrorDetails(code, message, null), Instant.now(), correlationId);
    }

    public static <T> ApiResponse<T> error(ErrorDetails errorDetails, String correlationId) {
        return new ApiResponse<>(false, null, errorDetails, Instant.now(), correlationId);
    }

    public record ErrorDetails(
        String code,
        String message,
        java.util.Map<String, String> fieldErrors
    ) {}
}
