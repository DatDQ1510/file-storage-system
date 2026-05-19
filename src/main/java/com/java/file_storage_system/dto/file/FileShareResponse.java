package com.java.file_storage_system.dto.file;

import com.java.file_storage_system.constant.FileSharePermission;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO cho FileShare entity
 */
public record FileShareResponse(
        String id,
        String fileId,
        String sharedWithUserId,
        String sharedByUserId,
        FileSharePermission permission,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}
