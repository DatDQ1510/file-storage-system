package com.java.file_storage_system.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseException{

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public ConflictException(String resourceName, String fieldName, Object fieldValue) {

        this(String.format("%s with %s '%s' already exists.", resourceName, fieldName, fieldValue));
    }

    public static ConflictException alreadyExists(String resourceName, String fieldName, Object fieldValue) {
        return new ConflictException(resourceName, fieldName, fieldValue);
    }

    public static ConflictException withMessage(String message) {
        return new ConflictException(message);
    }
}
