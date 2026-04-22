package com.java.file_storage_system.dto.folder;

import java.util.List;

public record CreateFolderWithAclResponse(
        FolderResponse folder,
        List<FolderAclItemResponse> aclEntries
) {
}
