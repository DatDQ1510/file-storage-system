package com.java.file_storage_system.dto.chunk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChunkPreSignItemRequest(
        @NotBlank(message = "hash is required")
        String hash,

        @NotNull(message = "size is required")
        @Positive(message = "size must be greater than 0")
        Long size
) {
}
