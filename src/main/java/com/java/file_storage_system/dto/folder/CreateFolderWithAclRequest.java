package com.java.file_storage_system.dto.folder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateFolderWithAclRequest(
        @NotBlank(message = "nameFolder is required")
        String nameFolder,
        String path,
        String parentFolderId,
        @Valid
        List<FolderAclItemRequest> aclEntries
) {
}
