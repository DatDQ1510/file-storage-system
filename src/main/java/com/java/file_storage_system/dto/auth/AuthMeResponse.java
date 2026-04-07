package com.java.file_storage_system.dto.auth;

public record AuthMeResponse(
        String id,
        String username,
        String email
) {
}