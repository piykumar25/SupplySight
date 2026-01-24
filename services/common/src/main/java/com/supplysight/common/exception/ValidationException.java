package com.supplysight.common.exception;

import java.util.Map;

/**
 * Exception thrown for validation failures.
 */
public class ValidationException extends SupplySightException {
    
    private static final String ERROR_CODE = "VALIDATION_FAILED";
    private static final int HTTP_STATUS = 400;
    
    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
        this.fieldErrors = Map.of();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, ERROR_CODE, HTTP_STATUS);
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
