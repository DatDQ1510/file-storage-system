package com.java.file_storage_system.dto.tenant.activation;

public record TenantActivationCompleteResponse(
        String accessToken,
        String message,
        String role,
        String tenantId,
        String userId,
        String username,
        String email
) {
}
