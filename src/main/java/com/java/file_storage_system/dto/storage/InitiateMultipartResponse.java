package com.java.file_storage_system.dto.storage;

public record InitiateMultipartResponse(
    String uploadId,
    String objectName
) {}
