package com.java.file_storage_system.dto.chunk;

import java.io.Serializable;
import java.util.List;

public record FileAssemblyMessage(
        String messageId,
        String tenantId,
        String folderId,
        String ownerId,
        String fileName,
        String contentType,
        Long totalSize,
        List<String> chunkHashes,
        String requestedAt
) implements Serializable {
}
