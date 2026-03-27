package com.java.file_storage_system.service.impl;

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

    @Value("${app.security.forgot-password.code-ttl-minutes3}")
    private long forgotPasswordCodeTtlMinutes;

    @Value("${app.security.forgot-password.marker-ttl-minutes:5}")
    private long forgotPasswordMarkerTtlMinutes;

    @Value("${spring.mail.username:}")
    private String mailFromAddress;

    @Override
    public AuthTokens login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail().trim(),
                            request.getPassword()
                    )
            );

            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            return issueTokens(principal);
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid username/email or password");
        }
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

        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        String code = generate6DigitCode();
        String codeKey = getForgotPasswordCodeKey(normalizedEmail);
        String markerKey = getForgotPasswordMarkerKey(normalizedEmail);

        redisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(Math.max(3, forgotPasswordCodeTtlMinutes))); // thời hạn của digital code
        redisTemplate.opsForValue().set(markerKey, "1", Duration.ofMinutes(Math.max(5, forgotPasswordMarkerTtlMinutes))); // đánh dấu đã gửi email

        sendForgotPasswordMail(user.getEmail(), code);
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
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        user.setHashedPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        redisTemplate.delete(getForgotPasswordCodeKey(normalizedEmail));
        redisTemplate.delete(getForgotPasswordMarkerKey(normalizedEmail));
    }

    private AuthTokens issueTokens(CustomUserDetails principal) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(principal);

        return new AuthTokens(
                accessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationMs(),
                principal.getRole(),
                principal.getTenantId()
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
        message.setText("Your password reset code is: " + code + "\nThis code will expire in 2 minutes.");
        mailSender.send(message);
    }
}
