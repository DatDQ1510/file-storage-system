package com.java.file_storage_system.dto.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.java.file_storage_system.constant.FileStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateFileRequest(
        @NotBlank(message = "nameFile is required")
        String nameFile,

        @NotNull(message = "statusFile is required")
        FileStatus statusFile,

        @NotNull(message = "sizeFile is required")
        @Positive(message = "sizeFile must be greater than 0")
        Double sizeFile,

        JsonNode extraInfo,

        @NotBlank(message = "tenantId is required")
        String tenantId,

        @NotBlank(message = "folderId is required")
        String folderId,

        @NotBlank(message = "ownerId is required")
        String ownerId,

        String lockedByUserId
) {
}