package com.java.file_storage_system.dto.tenant;

import java.util.List;

public record AllTenantPageResponse(
        List<AllTenantResponse> items,
        int page,
        int offset,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
