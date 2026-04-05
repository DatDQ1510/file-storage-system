package com.java.file_storage_system.dto.fileVersion;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileVersionResponse(
        String id,
        Integer fileVersion,
        String fileHash,
        Double sizeFile,
        String fileId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}