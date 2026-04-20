package com.java.file_storage_system.dto.project.member;

import jakarta.validation.constraints.NotBlank;

public record AssignProjectMemberRequest(
        @NotBlank(message = "memberUserId is required")
        String memberUserId,
        Integer permission
) {
}