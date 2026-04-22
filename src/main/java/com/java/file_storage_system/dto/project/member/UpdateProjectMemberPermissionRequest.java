package com.java.file_storage_system.dto.project.member;

import jakarta.validation.constraints.NotNull;

public record UpdateProjectMemberPermissionRequest(
        @NotNull(message = "permission is required")
        Integer permission
) {
}
