package com.java.file_storage_system.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Record thiết lập cho response trả về thông tin project
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ProjectResponse(
        String id,
        String nameProject,
        String ownerId,
        String ownerName,
        String tenantId,
        String tenantName,
        String tenantAdminId,
        String tenantAdminName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String status
) {
}
