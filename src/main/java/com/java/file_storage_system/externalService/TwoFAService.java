package com.java.file_storage_system.externalService;

import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.dto.auth.TwoFASetupResponse;
import com.java.file_storage_system.dto.auth.TwoFAStatusResponse;
import com.java.file_storage_system.dto.auth.TwoFAVerifyRequest;
import com.java.file_storage_system.service.AuthService;

public interface TwoFAService {

    TwoFASetupResponse setupTwoFactor(CustomUserDetails principal);

    AuthService.AuthTokens verifyTwoFactor(TwoFAVerifyRequest request);

    TwoFAStatusResponse getTwoFactorStatus(CustomUserDetails principal);

    void disableTwoFactor(CustomUserDetails principal);
}