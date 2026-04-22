package com.java.file_storage_system.dto.folder;

public record FolderAclItemResponse(
                String id,
                String userId,
                String userName,
                /**
                 * Bitmask permission: 1=READ, 2=WRITE, 4=DELETE.
                 */
                Integer permission) {
}
