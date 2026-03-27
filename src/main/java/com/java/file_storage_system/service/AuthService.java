package com.java.file_storage_system.service;

import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.dto.auth.ForgotPasswordResetRequest;
import com.java.file_storage_system.dto.auth.ForgotPasswordSendCodeRequest;
import com.java.file_storage_system.dto.auth.ForgotPasswordVerifyCodeRequest;
import com.java.file_storage_system.dto.auth.LoginRequest;
import com.java.file_storage_system.dto.user.changePassword.ChangePasswordRequest;

public interface AuthService {

    AuthTokens login(LoginRequest request);

    AuthTokens refresh(String refreshToken);

    void changePassword(CustomUserDetails principal, ChangePasswordRequest request);

    void sendForgotPasswordCode(ForgotPasswordSendCodeRequest request);

    void verifyForgotPasswordCode(ForgotPasswordVerifyCodeRequest request);

    void resetForgotPassword(ForgotPasswordResetRequest request);

    record AuthTokens(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInMs,
            String role,
            String tenantId
    ) {
    }
}
