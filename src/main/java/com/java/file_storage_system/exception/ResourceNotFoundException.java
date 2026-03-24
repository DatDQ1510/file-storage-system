package com.java.file_storage_system.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        this(String.format("%s not found with %s : '%s'", resource, field, value));
    }

    public static ResourceNotFoundException byField(String resource, String field, Object value) {
        return new ResourceNotFoundException(resource, field, value);
    }

    public static ResourceNotFoundException withMessage(String message) {
        return new ResourceNotFoundException(message);
    }
}
