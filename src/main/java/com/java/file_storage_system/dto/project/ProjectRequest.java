package com.java.file_storage_system.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Record thiết lập cho request tạo project
 */
public record ProjectRequest(
        @NotBlank(message = "nameProject is required")
        @Size(max = 255, message = "nameProject must be at most 255 characters")
        String nameProject,

        @NotBlank(message = "ownerId is required")
        String ownerId
) {
}
