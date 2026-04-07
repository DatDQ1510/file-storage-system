package com.java.file_storage_system.dto.auth;

public record TwoFASetupResponse(
        boolean enabled,
        String otpauthUrl
) {
}