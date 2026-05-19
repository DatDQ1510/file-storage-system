package com.java.file_storage_system.dto.chunk;

public record ChunkPreSignItemResponse(
        String hash,
        Long size,
        String status,
        String bucket,
        String objectName,
        String uploadUrl,
        String existingObjectUrl,
        String expiresAt
) {
}
