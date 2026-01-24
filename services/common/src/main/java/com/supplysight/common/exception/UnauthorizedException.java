package com.supplysight.common.exception;

/**
 * Exception thrown for authentication failures.
 */
public class UnauthorizedException extends SupplySightException {
    
    private static final String ERROR_CODE = "UNAUTHORIZED";
    private static final int HTTP_STATUS = 401;

    public UnauthorizedException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }

    public UnauthorizedException() {
        super("Authentication required", ERROR_CODE, HTTP_STATUS);
    }
}
