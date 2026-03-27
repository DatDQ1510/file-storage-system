package com.java.file_storage_system.dto.project.member;

public record ProjectMemberResponse(
        String id,
        String projectId,
        String userId,
        String userName,
        Integer permission,
        String grantedByUserId
) {
}
