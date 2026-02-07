package com.supplysight.common.exception;

/** Exception thrown when attempting to create a duplicate resource. */
public class DuplicateResourceException extends SupplySightException {

    private static final String ERROR_CODE = "DUPLICATE_RESOURCE";
    private static final int HTTP_STATUS = 409;

    public DuplicateResourceException(String resourceType, String identifier) {
        super(
                String.format("%s already exists: %s", resourceType, identifier),
                ERROR_CODE,
                HTTP_STATUS);
    }

    public DuplicateResourceException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }
}
