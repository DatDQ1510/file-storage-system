package com.java.file_storage_system.dto.filechunkmap;

import java.time.LocalDateTime;

public record FileChunkMapResponse(
        String id,
        String versionId,
        String chunkId,
        Integer orderIndex,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
