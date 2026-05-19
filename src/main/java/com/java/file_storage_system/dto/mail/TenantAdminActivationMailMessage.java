package com.java.file_storage_system.dto.mail;

public record TenantAdminActivationMailMessage(
        String messageId,
        String tenantAdminId,
        String tenantId,
        String tenantName,
        String toEmail,
        String username,
        String token,
        long ttlHours,
        String createdAt
) {
}
