package com.java.file_storage_system.dto.chunk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChunkPreSignBatchRequest(
        @NotBlank(message = "tenantId is required")
        String tenantId,

        @NotEmpty(message = "chunks is required")
        @Size(max = 200, message = "chunks size must be less than or equal to 200")
        List<@Valid ChunkPreSignItemRequest> chunks,

        @Min(value = 1, message = "expiryMinutes must be at least 1")
        @Max(value = 10080, message = "expiryMinutes must be less than or equal to 10080")
        Integer expiryMinutes
) {
}
