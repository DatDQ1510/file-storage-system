package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.custom.CustomUserDetailsService;
import com.java.file_storage_system.custom.JwtTokenProvider;
import com.java.file_storage_system.dto.auth.LoginRequest;
import com.java.file_storage_system.exception.UnauthorizedException;
import com.java.file_storage_system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

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
}
