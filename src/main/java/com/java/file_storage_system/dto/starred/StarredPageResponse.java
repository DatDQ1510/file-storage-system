package com.java.file_storage_system.dto.starred;

import java.util.List;

public record StarredPageResponse(
        List<StarredFolderResponse> folders,
        List<StarredFileResponse> files
) {
}