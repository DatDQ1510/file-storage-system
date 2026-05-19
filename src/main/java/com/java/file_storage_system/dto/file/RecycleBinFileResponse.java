package com.java.file_storage_system.dto.file;

import com.java.file_storage_system.constant.FileStatus;

import java.time.LocalDateTime;

public record RecycleBinFileResponse(
        String id,
        String fileName,
        Double sizeFile,
        FileStatus statusFile,
        String folderId,
        String folderPath,
        String projectId,
        LocalDateTime deletedAt,
        LocalDateTime updatedAt
) {
}
