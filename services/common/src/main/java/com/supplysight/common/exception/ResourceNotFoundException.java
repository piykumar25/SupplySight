package com.supplysight.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends SupplySightException {
    
    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";
    private static final int HTTP_STATUS = 404;

    public ResourceNotFoundException(String resourceType, Object id) {
        super(String.format("%s not found with id: %s", resourceType, id), ERROR_CODE, HTTP_STATUS);
    }

    public ResourceNotFoundException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }
}
