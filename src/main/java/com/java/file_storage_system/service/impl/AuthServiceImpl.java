package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.custom.CustomUserDetailsService;
import com.java.file_storage_system.custom.JwtTokenProvider;
import com.java.file_storage_system.dto.auth.AuthMeResponse;
import com.java.file_storage_system.dto.auth.ForgotPasswordSendCodeRequest;
import com.java.file_storage_system.dto.auth.ForgotPasswordVerifyCodeRequest;
import com.java.file_storage_system.dto.auth.ForgotPasswordResetRequest;
import com.java.file_storage_system.dto.auth.LoginRequest;
import com.java.file_storage_system.dto.auth.UpdateProfileRequest;
import com.java.file_storage_system.dto.user.changePassword.ChangePasswordRequest;
import com.java.file_storage_system.entity.SystemAdminEntity;
import com.java.file_storage_system.entity.TenantAdminEntity;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.exception.UnauthorizedException;
import com.java.file_storage_system.repository.SystemAdminRepository;
import com.java.file_storage_system.repository.TenantAdminRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
 import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String FORGOT_PASSWORD_CODE_PREFIX = "auth:forgot-password:code:";
    private static final String FORGOT_PASSWORD_MARKER_PREFIX = "auth:forgot-password:marker:";

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SystemAdminRepository systemAdminRepository;
    private final TenantAdminRepository tenantAdminRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final JavaMailSender mailSender;

    private static final int forgotPasswordCodeTtlMinutes = 10;

    private static final int forgotPasswordMarkerTtlMinutes = 15;

    @Value("${spring.mail.username:}")
    private String mailFromAddress;

    @Override
    public LoginResult login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail().trim(),
                            request.getPassword()
                    )
            );

            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            if (hasTwoFactorEnabled(principal)) {
                return LoginResult.challenge(buildTwoFactorChallenge(principal));
            }

            return LoginResult.token(issueTokens(principal));
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid username/email or password");
        }
    }

    @Override
    public AuthTokens issueTokens(CustomUserDetails principal) {
        return issueTokensForPrincipal(principal);
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        CustomUserDetails principal = (CustomUserDetails) customUserDetailsService.loadUserByUsername(username);
        return issueTokens(principal);
    }

    @Override
    @Transactional
    public void changePassword(CustomUserDetails principal, ChangePasswordRequest request) {
        String currentPassword = request.currentPassword();
        String newPassword = request.newPassword();

        if (currentPassword.equals(newPassword)) {
            throw new ConflictException("New password must be different from old password");
        }

        switch (principal.getRole()) {
            case "SYSTEM_ADMIN" -> changeSystemAdminPassword(principal.getId(), currentPassword, newPassword);
            case "TENANT_ADMIN" -> changeTenantAdminPassword(principal.getId(), currentPassword, newPassword);
            case "USER" -> changeUserPassword(principal.getId(), currentPassword, newPassword);
            default -> throw new UnauthorizedException("Unsupported account role");
        }
    }

    @Override
    @Transactional
    public void sendForgotPasswordCode(ForgotPasswordSendCodeRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        boolean emailExists = hasForgotPasswordAccountByEmail(normalizedEmail);

        String code = generate6DigitCode();
        String codeKey = getForgotPasswordCodeKey(normalizedEmail);
        String markerKey = getForgotPasswordMarkerKey(normalizedEmail);

        // Always set marker/code to avoid leaking whether an email exists.
        redisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(forgotPasswordCodeTtlMinutes));
        redisTemplate.opsForValue().set(markerKey, "1", Duration.ofMinutes(forgotPasswordMarkerTtlMinutes));

        if (emailExists) {
            sendForgotPasswordMail(normalizedEmail, code);
        }
    }

    private boolean hasForgotPasswordAccountByEmail(String normalizedEmail) {
        return userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()
                || tenantAdminRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()
                || systemAdminRepository.findByEmailIgnoreCase(normalizedEmail).isPresent();
    }

        private boolean hasTwoFactorEnabled(CustomUserDetails principal) {
        UserRole role = UserRole.fromString(principal.getRole());

        return switch (role) {
            case USER -> userRepository.findById(principal.getId())
                .map(UserEntity::getSecretKey)
                .map(this::isNotBlank)
                .orElse(false);
            case TENANT_ADMIN -> tenantAdminRepository.findById(principal.getId())
                .map(TenantAdminEntity::getSecretKey)
                .map(this::isNotBlank)
                .orElse(false);
            case SYSTEM_ADMIN -> systemAdminRepository.findById(principal.getId())
                .map(SystemAdminEntity::getSecretKey)
                .map(this::isNotBlank)
                .orElse(false);
        };
        }

        private TwoFactorChallenge buildTwoFactorChallenge(CustomUserDetails principal) {
        return new TwoFactorChallenge(
            true,
            true,
            true,
            principal.getId(),
            principal.getUsername(),
            principal.getEmail(),
            principal.getRole(),
            principal.getTenantId()
        );
        }

    @Override
    @Transactional(readOnly = true)
    public void verifyForgotPasswordCode(ForgotPasswordVerifyCodeRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String codeKey = getForgotPasswordCodeKey(normalizedEmail);
        String markerKey = getForgotPasswordMarkerKey(normalizedEmail);

        Object storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            Boolean hasMarker = redisTemplate.hasKey(markerKey);
            if (Boolean.TRUE.equals(hasMarker)) {
                throw new UnauthorizedException("Verification code has expired");
            }
            throw new ResourceNotFoundException("Verification code not found");
        }

        if (!request.code().equals(storedCode.toString())) {
            throw new UnauthorizedException("Verification code is invalid");
        }
    }

    @Override
    @Transactional
    public void resetForgotPassword(ForgotPasswordResetRequest request) {
        verifyForgotPasswordCode(new ForgotPasswordVerifyCodeRequest(request.email(), request.code()));

        String normalizedEmail = normalizeEmail(request.email());
        String encodedPassword = passwordEncoder.encode(request.newPassword());

        if (!updateForgotPasswordForAccountByEmail(normalizedEmail, encodedPassword)) {
            throw new ResourceNotFoundException("Email not found");
        }

        redisTemplate.delete(getForgotPasswordCodeKey(normalizedEmail));
        redisTemplate.delete(getForgotPasswordMarkerKey(normalizedEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthMeResponse getBasicUserInfoById(UUID userId) {
        String id = userId.toString();

        UserEntity user = userRepository.findById(id).orElse(null);
        if (user != null) {
            return new AuthMeResponse(
                    user.getId(),
                    user.getUserName(),
                    user.getEmail(),
                    isNotBlank(user.getSecretKey())
            );
        }

        TenantAdminEntity tenantAdmin = tenantAdminRepository.findById(id).orElse(null);
        if (tenantAdmin != null) {
            return new AuthMeResponse(
                    tenantAdmin.getId(),
                    tenantAdmin.getUserName(),
                    tenantAdmin.getEmail(),
                    isNotBlank(tenantAdmin.getSecretKey())
            );
        }

        SystemAdminEntity systemAdmin = systemAdminRepository.findById(id).orElse(null);
        if (systemAdmin != null) {
            return new AuthMeResponse(
                    systemAdmin.getId(),
                    systemAdmin.getUserName(),
                    systemAdmin.getEmail(),
                    isNotBlank(systemAdmin.getSecretKey())
            );
        }

        throw ResourceNotFoundException.byField("User", "id", id);
    }

    @Override
    @Transactional
    public AuthMeResponse updateProfile(CustomUserDetails principal, UpdateProfileRequest request) {
        String normalizedUserName = normalizeUserName(request.username());
        String normalizedEmail = normalizeEmail(request.email());

        switch (principal.getRole()) {
            case "USER" -> updateUserProfile(principal.getId(), principal.getTenantId(), normalizedUserName, normalizedEmail);
            case "TENANT_ADMIN" -> updateTenantAdminProfile(principal.getId(), normalizedUserName, normalizedEmail);
            case "SYSTEM_ADMIN" -> updateSystemAdminProfile(principal.getId(), normalizedUserName, normalizedEmail);
            default -> throw new UnauthorizedException("Unsupported account role");
        }

        return getBasicUserInfoById(UUID.fromString(principal.getId()));
    }

    private void updateUserProfile(String userId, String tenantId, String normalizedUserName, String normalizedEmail) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (tenantId == null || tenantId.isBlank()) {
            throw new UnauthorizedException("Tenant scope is required for user profile update");
        }

        if (!user.getUserName().equalsIgnoreCase(normalizedUserName)
                && userRepository.existsByUserNameIgnoreCaseAndTenantId(normalizedUserName, tenantId)) {
            throw new ConflictException("Username already exists in tenant: " + normalizedUserName);
        }

        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)
                && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("User email already exists: " + normalizedEmail);
        }

        user.setUserName(normalizedUserName);
        user.setEmail(normalizedEmail);
        userRepository.save(user);
    }

    private void updateTenantAdminProfile(String tenantAdminId, String normalizedUserName, String normalizedEmail) {
        TenantAdminEntity tenantAdmin = tenantAdminRepository.findById(tenantAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant admin not found with id: " + tenantAdminId));

        if (!tenantAdmin.getUserName().equalsIgnoreCase(normalizedUserName)
                && tenantAdminRepository.existsByUserNameIgnoreCase(normalizedUserName)) {
            throw new ConflictException("Tenant admin userName already exists: " + normalizedUserName);
        }

        if (!tenantAdmin.getEmail().equalsIgnoreCase(normalizedEmail)
                && tenantAdminRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Tenant admin email already exists: " + normalizedEmail);
        }

        tenantAdmin.setUserName(normalizedUserName);
        tenantAdmin.setEmail(normalizedEmail);
        tenantAdminRepository.save(tenantAdmin);
    }

    private void updateSystemAdminProfile(String systemAdminId, String normalizedUserName, String normalizedEmail) {
        SystemAdminEntity systemAdmin = systemAdminRepository.findById(systemAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("System admin not found with id: " + systemAdminId));

        if (!systemAdmin.getUserName().equalsIgnoreCase(normalizedUserName)
                && systemAdminRepository.existsByUserNameIgnoreCase(normalizedUserName)) {
            throw new ConflictException("System admin username already exists: " + normalizedUserName);
        }

        if (!systemAdmin.getEmail().equalsIgnoreCase(normalizedEmail)
                && systemAdminRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ConflictException("System admin email already exists: " + normalizedEmail);
        }

        systemAdmin.setUserName(normalizedUserName);
        systemAdmin.setEmail(normalizedEmail);
        systemAdminRepository.save(systemAdmin);
    }

    private boolean updateForgotPasswordForAccountByEmail(String normalizedEmail, String encodedPassword) {
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user != null) {
            user.setHashedPassword(encodedPassword);
            userRepository.save(user);
            return true;
        }

        TenantAdminEntity tenantAdmin = tenantAdminRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (tenantAdmin != null) {
            tenantAdmin.setHashedPassword(encodedPassword);
            tenantAdminRepository.save(tenantAdmin);
            return true;
        }

        SystemAdminEntity systemAdmin = systemAdminRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (systemAdmin != null) {
            systemAdmin.setHashedPassword(encodedPassword);
            systemAdminRepository.save(systemAdmin);
            return true;
        }

        return false;
    }

    private AuthTokens issueTokensForPrincipal(CustomUserDetails principal) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(principal);

        return new AuthTokens(
                accessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationMs(),
                principal.getRole(),
                principal.getTenantId(),
                principal.getId(),
                principal.getUsername(),
                principal.getEmail()
        );
    }

    private void changeSystemAdminPassword(String systemAdminId, String currentPassword, String newPassword) {
        SystemAdminEntity account = systemAdminRepository.findById(systemAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("System admin not found with id: " + systemAdminId));

        validateOldPassword(account.getHashedPassword(), currentPassword);
        account.setHashedPassword(passwordEncoder.encode(newPassword));
        systemAdminRepository.save(account);
    }

    private void changeTenantAdminPassword(String tenantAdminId, String currentPassword, String newPassword) {
        TenantAdminEntity account = tenantAdminRepository.findById(tenantAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant admin not found with id: " + tenantAdminId));

        validateOldPassword(account.getHashedPassword(), currentPassword);
        account.setHashedPassword(passwordEncoder.encode(newPassword));
        tenantAdminRepository.save(account);
    }

    private void changeUserPassword(String userId, String currentPassword, String newPassword) {
        UserEntity account = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        validateOldPassword(account.getHashedPassword(), currentPassword);
        account.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(account);
    }

    private void validateOldPassword(String storedHash, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, storedHash)) {
            throw new UnauthorizedException("Old password is incorrect");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUserName(String userName) {
        return userName.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String generate6DigitCode() {
        int value = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%06d", value);
    }

    private String getForgotPasswordCodeKey(String normalizedEmail) {
        return FORGOT_PASSWORD_CODE_PREFIX + normalizedEmail;
    }

    private String getForgotPasswordMarkerKey(String normalizedEmail) {
        return FORGOT_PASSWORD_MARKER_PREFIX + normalizedEmail;
    }

    private void sendForgotPasswordMail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (mailFromAddress != null && !mailFromAddress.isBlank()) {
            message.setFrom(mailFromAddress);
        }
        message.setTo(toEmail);
        message.setSubject("Password Reset Verification Code");
        message.setText("Your password reset code is: " + code + "\nThis code will expire in 3 minutes.");
        mailSender.send(message);
    }
}
