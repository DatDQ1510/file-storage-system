package com.java.file_storage_system.dto.user.searchUser;

import java.util.List;

public record UserSearchPageResponse(
        List<UserSearchItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
