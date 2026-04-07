package com.java.file_storage_system.service;

import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.dto.auth.ForgotPasswordResetRequest;
import com.java.file_storage_system.dto.auth.ForgotPasswordSendCodeRequest;
import com.java.file_storage_system.dto.auth.ForgotPasswordVerifyCodeRequest;
import com.java.file_storage_system.dto.auth.LoginRequest;
import com.java.file_storage_system.dto.auth.AuthMeResponse;
import com.java.file_storage_system.dto.user.changePassword.ChangePasswordRequest;

import java.util.UUID;

public interface AuthService {

    LoginResult login(LoginRequest request);

    AuthTokens issueTokens(CustomUserDetails principal);

    AuthTokens refresh(String refreshToken);

    void changePassword(CustomUserDetails principal, ChangePasswordRequest request);

    void sendForgotPasswordCode(ForgotPasswordSendCodeRequest request);

    void verifyForgotPasswordCode(ForgotPasswordVerifyCodeRequest request);

    void resetForgotPassword(ForgotPasswordResetRequest request);

    AuthMeResponse getBasicUserInfoById(UUID userId);

    record AuthTokens(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInMs,
            String role,
            String tenantId,
            String userId,
            String username,
            String email
    ) {
    }

    record TwoFactorChallenge(
            boolean requiresTwoFactor,
            boolean twoFactorRequired,
            boolean requireTwoFactor,
            String userId,
            String username,
            String email,
            String role,
            String tenantId
    ) {
    }

    record LoginResult(
            AuthTokens tokens,
            TwoFactorChallenge challenge
    ) {
        public static LoginResult token(AuthTokens tokens) {
            return new LoginResult(tokens, null);
        }

        public static LoginResult challenge(TwoFactorChallenge challenge) {
            return new LoginResult(null, challenge);
        }

        public boolean requiresTwoFactor() {
            return challenge != null;
        }
    }
}
