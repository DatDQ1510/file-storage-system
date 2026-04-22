package com.java.file_storage_system.dto.folder;

public record FolderPathNodeResponse(
        String folderId,
        String nameFolder,
        String path,
        boolean hasChildren
) {
}
