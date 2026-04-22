package com.java.file_storage_system.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @NotBlank(message = "nameProject is required")
        @Size(max = 255, message = "nameProject must not exceed 255 characters")
        String nameProject
) {
}
