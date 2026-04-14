package com.java.file_storage_system.dto.user.allUser;

import java.util.List;

public record AllUserPageResponse(
        List<AllUserResponse> items,
        int page,
        int offset,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
