package com.java.file_storage_system.dto.folder;

import java.time.LocalDateTime;

public record FolderResponse(
        String id,
        String nameFolder,
        String path,
        String tenantId,
        String projectId,
        String ownerId,
        String parentFolderId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
