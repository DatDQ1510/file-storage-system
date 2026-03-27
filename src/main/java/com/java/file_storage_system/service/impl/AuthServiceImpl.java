package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.custom.CustomUserDetailsService;
import com.java.file_storage_system.custom.JwtTokenProvider;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SystemAdminRepository systemAdminRepository;
    private final TenantAdminRepository tenantAdminRepository;
    private final UserRepository userRepository;

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
}
