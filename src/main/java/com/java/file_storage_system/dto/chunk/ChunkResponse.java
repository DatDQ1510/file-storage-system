package com.java.file_storage_system.dto.chunk;

import java.time.LocalDateTime;

public record ChunkResponse(
        String id,
        Double sizeChunk,
        String minIOUrl,
        String chunkHash,
        String tenantId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
