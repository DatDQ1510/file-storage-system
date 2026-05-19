package com.java.file_storage_system.dto.file;

import com.java.file_storage_system.constant.FileSharePermission;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO để share file với user khác
 */
public record ShareFileRequest(
        @NotNull(message = "sharedWithUserId không được null")
        String sharedWithUserId,

        @NotNull(message = "permission không được null")
        FileSharePermission permission,

        /**
         * Optional: Ngày hết hạn chia sẻ
         * Nếu null → không expire (vĩnh viễn)
         */
        LocalDateTime expiresAt
) {}
