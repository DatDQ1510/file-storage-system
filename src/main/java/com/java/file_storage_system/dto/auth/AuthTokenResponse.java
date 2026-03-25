package com.java.file_storage_system.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthTokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInMs;
    private String role;
    private String tenantId;
}
