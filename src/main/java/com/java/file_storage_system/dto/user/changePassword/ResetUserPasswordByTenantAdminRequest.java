package com.java.file_storage_system.dto.user.changePassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordByTenantAdminRequest(
        @NotBlank(message = "newPassword is required")
        @Size(min = 6, max = 100, message = "newPassword must be between 6 and 100 characters")
        String newPassword
) {
}
