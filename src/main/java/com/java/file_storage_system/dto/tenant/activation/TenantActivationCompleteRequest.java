package com.java.file_storage_system.dto.tenant.activation;

import jakarta.validation.constraints.NotBlank;

public record TenantActivationCompleteRequest(
        @NotBlank(message = "token is required")
        String token,

        @NotBlank(message = "password is required")
        String password,

        @NotBlank(message = "confirmPassword is required")
        String confirmPassword
) {
}
