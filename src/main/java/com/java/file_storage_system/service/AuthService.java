package com.java.file_storage_system.service;

import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.dto.auth.LoginRequest;
import com.java.file_storage_system.dto.user.changePassword.ChangePasswordRequest;

public interface AuthService {

    AuthTokens login(LoginRequest request);

    AuthTokens refresh(String refreshToken);

    void changePassword(CustomUserDetails principal, ChangePasswordRequest request);

    record AuthTokens(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInMs,
            String role,
            String tenantId
    ) {
    }
}
