package com.java.file_storage_system.dto.starred;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StarredFolderResponse(
        String starId,
        String folderId,
        String folderName,
        String path,
        String projectId,
        LocalDateTime starredAt,
        LocalDateTime folderUpdatedAt
) {
}