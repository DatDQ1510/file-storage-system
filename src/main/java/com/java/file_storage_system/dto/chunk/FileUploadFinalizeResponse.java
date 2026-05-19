package com.java.file_storage_system.dto.chunk;

public record FileUploadFinalizeResponse(
        String status,
        String messageId,
        String exchange,
        String routingKey,
        Integer totalChunks
) {
}
