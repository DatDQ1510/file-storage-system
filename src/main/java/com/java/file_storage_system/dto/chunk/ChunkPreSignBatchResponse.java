package com.java.file_storage_system.dto.chunk;

import java.util.List;

public record ChunkPreSignBatchResponse(
        List<ChunkPreSignItemResponse> items
) {
}
