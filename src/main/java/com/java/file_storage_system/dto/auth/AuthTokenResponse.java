package com.java.file_storage_system.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthTokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInMs;
    private String role;
    private String tenantId;
    private String userId;
    private String username;
    private String email;
    private String redirectUrl;
    private String userDisplayName;
}
