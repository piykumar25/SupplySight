package com.supplysight.common.exception;

/**
 * Exception thrown for authorization failures.
 */
public class ForbiddenException extends SupplySightException {
    
    private static final String ERROR_CODE = "FORBIDDEN";
    private static final int HTTP_STATUS = 403;

    public ForbiddenException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }

    public ForbiddenException() {
        super("Access denied", ERROR_CODE, HTTP_STATUS);
    }
}
