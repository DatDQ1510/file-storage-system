package com.java.file_storage_system.dto.tenant.activation;

import java.time.Instant;

public record TenantActivationTokenInfo(
        boolean isValid,
        String message,
        String companyName,
        String subdomain,
        String adminEmail,
        Instant expiresAt
) {
    public static TenantActivationTokenInfo invalid(String message) {
        return new TenantActivationTokenInfo(false, message, null, null, null, null);
    }
}
