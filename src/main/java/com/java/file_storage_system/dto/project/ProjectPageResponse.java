package com.java.file_storage_system.dto.project;

import java.util.List;

public record ProjectPageResponse(
        List<ProjectResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
