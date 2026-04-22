package com.java.file_storage_system.dto.folder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FolderAclItemRequest(
                @NotBlank(message = "userId is required") String userId,
                /**
                 * Bitmask permission: 1=READ, 2=WRITE, 4=DELETE (max 7 = all three).
                 */
                @NotNull(message = "permission is required") @Min(value = 1, message = "permission must be at least 1 (READ)") @Max(value = 7, message = "permission must be at most 7 (READ+WRITE+DELETE)") Integer permission) {
}
