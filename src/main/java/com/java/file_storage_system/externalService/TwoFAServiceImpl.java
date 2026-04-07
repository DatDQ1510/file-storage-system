package com.java.file_storage_system.externalService;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.dto.auth.TwoFASetupResponse;
import com.java.file_storage_system.dto.auth.TwoFAStatusResponse;
import com.java.file_storage_system.dto.auth.TwoFAVerifyRequest;
import com.java.file_storage_system.service.AuthService;
import com.java.file_storage_system.entity.SystemAdminEntity;
import com.java.file_storage_system.entity.TenantAdminEntity;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.exception.UnauthorizedException;
import com.java.file_storage_system.repository.SystemAdminRepository;
import com.java.file_storage_system.repository.TenantAdminRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class TwoFAServiceImpl implements TwoFAService {

    private final GoogleAuthenticator googleAuthenticator;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final TenantAdminRepository tenantAdminRepository;
    private final SystemAdminRepository systemAdminRepository;

    @Value("${app.security.two-factor.issuer:file-storage-system}")
    private String issuer;

    @Override
    @Transactional
    public TwoFASetupResponse setupTwoFactor(CustomUserDetails principal) {
        TwoFAAccountContext account = loadAccountOrThrow(principal);

        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        account.setSecretKey(key.getKey());
        account.persist();

        String otpauthUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, account.getEmail(), key);
        return new TwoFASetupResponse(true, otpauthUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthService.AuthTokens verifyTwoFactor(TwoFAVerifyRequest request) {
        TwoFAAccountContext account = loadAccountByEmailOrThrow(request.email());
        String secretKey = account.getSecretKey();

        if (secretKey == null || secretKey.isBlank()) {
            throw new ResourceNotFoundException("2FA secret is not configured for this account");
        }

        int code = Integer.parseInt(request.code());
        if (!googleAuthenticator.authorize(secretKey, code)) {
            throw new UnauthorizedException("Mã OTP không đúng");
        }

        return authService.issueTokens(account.getPrincipal());
    }

    @Override
    @Transactional(readOnly = true)
    public TwoFAStatusResponse getTwoFactorStatus(CustomUserDetails principal) {
        TwoFAAccountContext account = loadAccountOrThrow(principal);
        return new TwoFAStatusResponse(hasTwoFactorEnabled(account.getSecretKey()));
    }

    @Override
    @Transactional
    public void disableTwoFactor(CustomUserDetails principal) {
        TwoFAAccountContext account = loadAccountOrThrow(principal);
        account.setSecretKey(null);
        account.persist();
    }

    private TwoFAAccountContext loadAccountOrThrow(CustomUserDetails principal) {
        String accountId = principal.getId();
        UserRole role = UserRole.fromString(principal.getRole());

        return switch (role) {
            case USER -> loadUserAccountOrThrow(accountId);
            case TENANT_ADMIN -> loadTenantAdminAccountOrThrow(accountId);
            case SYSTEM_ADMIN -> loadSystemAdminAccountOrThrow(accountId);
        };
    }

    private TwoFAAccountContext loadUserAccountOrThrow(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return new TwoFAAccountContext(
            toPrincipal(user),
                user.getEmail(),
                user.getSecretKey(),
                user::setSecretKey,
                () -> userRepository.save(user)
        );
    }

    private TwoFAAccountContext loadTenantAdminAccountOrThrow(String tenantAdminId) {
        TenantAdminEntity tenantAdmin = tenantAdminRepository.findById(tenantAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant admin not found with id: " + tenantAdminId));

        return new TwoFAAccountContext(
            toPrincipal(tenantAdmin),
                tenantAdmin.getEmail(),
                tenantAdmin.getSecretKey(),
                tenantAdmin::setSecretKey,
                () -> tenantAdminRepository.save(tenantAdmin)
        );
    }

    private TwoFAAccountContext loadSystemAdminAccountOrThrow(String systemAdminId) {
        SystemAdminEntity systemAdmin = systemAdminRepository.findById(systemAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("System admin not found with id: " + systemAdminId));

        return new TwoFAAccountContext(
            toPrincipal(systemAdmin),
                systemAdmin.getEmail(),
                systemAdmin.getSecretKey(),
                systemAdmin::setSecretKey,
                () -> systemAdminRepository.save(systemAdmin)
        );
    }

    private TwoFAAccountContext loadAccountByEmailOrThrow(String email) {
        String normalizedEmail = normalizeEmail(email);

        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user != null) {
            return new TwoFAAccountContext(
                    toPrincipal(user),
                    user.getEmail(),
                    user.getSecretKey(),
                    user::setSecretKey,
                    () -> userRepository.save(user)
            );
        }

        TenantAdminEntity tenantAdmin = tenantAdminRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (tenantAdmin != null) {
            return new TwoFAAccountContext(
                    toPrincipal(tenantAdmin),
                    tenantAdmin.getEmail(),
                    tenantAdmin.getSecretKey(),
                    tenantAdmin::setSecretKey,
                    () -> tenantAdminRepository.save(tenantAdmin)
            );
        }

        SystemAdminEntity systemAdmin = systemAdminRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (systemAdmin != null) {
            return new TwoFAAccountContext(
                    toPrincipal(systemAdmin),
                    systemAdmin.getEmail(),
                    systemAdmin.getSecretKey(),
                    systemAdmin::setSecretKey,
                    () -> systemAdminRepository.save(systemAdmin)
            );
        }

        throw new ResourceNotFoundException("Account not found with email: " + email);
    }

    private boolean hasTwoFactorEnabled(String secretKey) {
        return secretKey != null && !secretKey.isBlank();
    }

    private CustomUserDetails toPrincipal(UserEntity user) {
        return new CustomUserDetails(
                user.getId(),
                user.getUserName(),
                user.getHashedPassword(),
                UserRole.USER.name(),
                user.getTenant() == null ? null : user.getTenant().getId(),
                user.getEmail()
        );
    }

    private CustomUserDetails toPrincipal(TenantAdminEntity tenantAdmin) {
        return new CustomUserDetails(
                tenantAdmin.getId(),
                tenantAdmin.getUserName(),
                tenantAdmin.getHashedPassword(),
                UserRole.TENANT_ADMIN.name(),
                tenantAdmin.getTenant() == null ? null : tenantAdmin.getTenant().getId(),
                tenantAdmin.getEmail()
        );
    }

    private CustomUserDetails toPrincipal(SystemAdminEntity systemAdmin) {
        return new CustomUserDetails(
                systemAdmin.getId(),
                systemAdmin.getUserName(),
                systemAdmin.getHashedPassword(),
                UserRole.SYSTEM_ADMIN.name(),
                null,
                systemAdmin.getEmail()
        );
    }

    private static class TwoFAAccountContext {
        private final CustomUserDetails principal;
        private final String email;
        private String secretKey;
        private final Consumer<String> secretWriter;
        private final Runnable persistenceAction;

        private TwoFAAccountContext(
                CustomUserDetails principal,
                String email,
                String secretKey,
                Consumer<String> secretWriter,
                Runnable persistenceAction
        ) {
            this.principal = principal;
            this.email = email;
            this.secretKey = secretKey;
            this.secretWriter = secretWriter;
            this.persistenceAction = persistenceAction;
        }

        private CustomUserDetails getPrincipal() {
            return principal;
        }

        private String getEmail() {
            return email;
        }

        private String getSecretKey() {
            return secretKey;
        }

        private void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
            this.secretWriter.accept(secretKey);
        }

        private void persist() {
            this.persistenceAction.run();
        }
    }
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}