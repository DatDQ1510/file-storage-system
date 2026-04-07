package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.custom.CustomUserDetailsService;
import com.java.file_storage_system.custom.JwtTokenProvider;
import com.java.file_storage_system.dto.auth.ForgotPasswordSendCodeRequest;
import com.java.file_storage_system.dto.auth.ForgotPasswordVerifyCodeRequest;
import com.java.file_storage_system.dto.auth.ForgotPasswordResetRequest;
import com.java.file_storage_system.dto.auth.LoginRequest;
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
        String oldPassword = request.oldPassword();
        String newPassword = request.newPassword();

        if (oldPassword.equals(newPassword)) {
            throw new ConflictException("New password must be different from old password");
        }

        switch (principal.getRole()) {
            case "SYSTEM_ADMIN" -> changeSystemAdminPassword(principal.getId(), oldPassword, newPassword);
            case "TENANT_ADMIN" -> changeTenantAdminPassword(principal.getId(), oldPassword, newPassword);
            case "USER" -> changeUserPassword(principal.getId(), oldPassword, newPassword);
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

    private void changeSystemAdminPassword(String systemAdminId, String oldPassword, String newPassword) {
        SystemAdminEntity account = systemAdminRepository.findById(systemAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("System admin not found with id: " + systemAdminId));

        validateOldPassword(account.getHashedPassword(), oldPassword);
        account.setHashedPassword(passwordEncoder.encode(newPassword));
        systemAdminRepository.save(account);
    }

    private void changeTenantAdminPassword(String tenantAdminId, String oldPassword, String newPassword) {
        TenantAdminEntity account = tenantAdminRepository.findById(tenantAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant admin not found with id: " + tenantAdminId));

        validateOldPassword(account.getHashedPassword(), oldPassword);
        account.setHashedPassword(passwordEncoder.encode(newPassword));
        tenantAdminRepository.save(account);
    }

    private void changeUserPassword(String userId, String oldPassword, String newPassword) {
        UserEntity account = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        validateOldPassword(account.getHashedPassword(), oldPassword);
        account.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(account);
    }

    private void validateOldPassword(String storedHash, String oldPassword) {
        if (!passwordEncoder.matches(oldPassword, storedHash)) {
            throw new UnauthorizedException("Old password is incorrect");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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
