package com.java.file_storage_system.dto.chunk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateChunkRequest(
        @NotNull(message = "Chunk size is required")
        @Positive(message = "Chunk size must be greater than 0")
        Double sizeChunk,

        @NotBlank(message = "MinIO URL is required")
        String minIOUrl,

        @NotBlank(message = "Chunk hash is required")
        String chunkHash,

        @NotBlank(message = "Tenant id is required")
        String tenantId
) {
}
