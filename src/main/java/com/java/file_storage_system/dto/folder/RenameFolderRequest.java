package com.java.file_storage_system.dto.folder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameFolderRequest(
        @NotBlank(message = "nameFolder is required")
        @Size(max = 255, message = "nameFolder must not exceed 255 characters")
        String nameFolder
) {
}
