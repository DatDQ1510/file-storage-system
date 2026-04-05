package com.java.file_storage_system.dto.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.java.file_storage_system.constant.FileStatus;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileResponse(
        String id,
        String nameFile,
        FileStatus statusFile,
        Double sizeFile,
        JsonNode extraInfo,
        String tenantId,
        String folderId,
        String ownerId,
        String lockedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}