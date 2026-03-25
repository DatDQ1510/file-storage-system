package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.auth.LoginRequest;

public interface AuthService {

    AuthTokens login(LoginRequest request);

    AuthTokens refresh(String refreshToken);

    record AuthTokens(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInMs,
            String role,
            String tenantId
    ) {
    }
}
