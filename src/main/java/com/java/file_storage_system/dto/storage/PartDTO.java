package com.java.file_storage_system.dto.storage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PartDTO(
    @Min(value = 1, message = "partNumber must be greater than 0")
    int partNumber,
    @NotBlank(message = "etag is required")
    String etag
) {}
