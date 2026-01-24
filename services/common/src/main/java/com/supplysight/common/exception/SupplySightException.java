package com.supplysight.common.exception;

/**
 * Base exception for all application-specific exceptions.
 */
public class SupplySightException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;

    public SupplySightException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public SupplySightException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
