package com.java.file_storage_system.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(
            String.format("%s not found with %s : '%s'", resource, field, value),
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND"
        );
    }
}
