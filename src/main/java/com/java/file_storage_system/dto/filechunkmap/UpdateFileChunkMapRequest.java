package com.java.file_storage_system.dto.filechunkmap;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record UpdateFileChunkMapRequest(
        @NotBlank(message = "Version id is required")
        String versionId,

        @NotBlank(message = "Chunk id is required")
        String chunkId,

        @NotNull(message = "Order index is required")
        Integer orderIndex
) {
}
