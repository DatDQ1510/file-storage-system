package com.java.file_storage_system.dto.chunk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record FileUploadFinalizeRequest(
        @NotBlank(message = "tenantId is required")
        String tenantId,

        @NotBlank(message = "folderId is required")
        String folderId,

        @NotBlank(message = "ownerId is required")
        String ownerId,

        @NotBlank(message = "fileName is required")
        String fileName,

        String contentType,

        @NotNull(message = "totalSize is required")
        @Positive(message = "totalSize must be greater than 0")
        Long totalSize,

        @NotEmpty(message = "chunkHashes is required")
        List<@NotBlank(message = "chunk hash must not be blank") String> chunkHashes
) {
}
