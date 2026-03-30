package com.java.file_storage_system.dto.filechunkmap;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateFileChunkMapRequest(
        @NotNull(message = "Version id is required")
        @Min(value = 1, message = "Version id must be greater than or equal to 1")
        Integer versionId,

        @NotNull(message = "Chunk id is required")
        @Min(value = 1, message = "Chunk id must be greater than or equal to 1")
        Integer chunkId,

        @NotNull(message = "Order index is required")
        @Min(value = 0, message = "Order index must be greater than or equal to 0")
        Integer orderIndex
) {
}
