package com.java.file_storage_system.dto.file;

import jakarta.validation.constraints.NotBlank;

public record RenameFileRequest(
    @NotBlank(message = "nameFile is required")
    String nameFile
) {
}
