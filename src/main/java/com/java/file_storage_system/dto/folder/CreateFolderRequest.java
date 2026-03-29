package com.java.file_storage_system.dto.folder;

import jakarta.validation.constraints.NotBlank;

public record CreateFolderRequest(
        @NotBlank(message = "nameFolder is required")
        String nameFolder,
        String path,
        @NotBlank(message = "tenantId is required")
        String tenantId,
        @NotBlank(message = "projectId is required")
        String projectId,
        @NotBlank(message = "ownerId is required")
        String ownerId,
        String parentFolderId
) {
}
