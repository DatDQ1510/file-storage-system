package com.java.file_storage_system.dto.project.member;

import jakarta.validation.constraints.NotBlank;

public record AddProjectMemberRequest(
        @NotBlank(message = "userId is required")
        String userId,
        Integer permission
) {
}
