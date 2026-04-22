package com.java.file_storage_system.dto.folder;

public record ProjectMemberForAclResponse(
        String userId,
        String userName,
        String email
) {
}
