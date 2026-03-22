package com.java.file_storage_system.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseException{

    public ConflictException(String resourceName, String fieldName, Object fieldValue) {

        super(
                String.format("%s with %s '%s' already exists.", resourceName, fieldName, fieldValue),
                HttpStatus.CONFLICT,
                "CONFLICT"
        );
    }
}
