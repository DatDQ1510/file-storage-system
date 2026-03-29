package com.java.file_storage_system.dto.folder;

import jakarta.validation.constraints.NotBlank;

public record UpdateFolderRequest(
        @NotBlank(message = "nameFolder is required")
        String nameFolder,
        @NotBlank(message = "path is required")
        String path,
        @NotBlank(message = "ownerId is required")
        String ownerId,
        String parentFolderId
) {
}
