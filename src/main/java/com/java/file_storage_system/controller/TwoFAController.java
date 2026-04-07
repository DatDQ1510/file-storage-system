package com.java.file_storage_system.controller;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.dto.auth.AuthTokenResponse;
import com.java.file_storage_system.dto.auth.TwoFASetupResponse;
import com.java.file_storage_system.dto.auth.TwoFAStatusResponse;
import com.java.file_storage_system.dto.auth.TwoFAVerifyRequest;
import com.java.file_storage_system.exception.UnauthorizedException;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.RolePermissionService;
import com.java.file_storage_system.externalService.TwoFAService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/2fa")
public class TwoFAController {

    private final TwoFAService twoFAService;
    private final RolePermissionService rolePermissionService;

    @Value("${app.security.refresh-cookie-name:refresh_token}")
    private String refreshCookieName;

    @Value("${app.security.jwt-refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.security.refresh-cookie-same-site:Lax}")
        private String refreshCookieSameSite;

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<TwoFASetupResponse>> setupTwoFactor(
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        CustomUserDetails principal = extractPrincipal(authentication);
        TwoFASetupResponse response = twoFAService.setupTwoFactor(principal);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Two-factor authentication setup successfully",
                        response,
                        httpServletRequest.getRequestURI()
                ));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> verifyTwoFactor(
            @Valid @RequestBody TwoFAVerifyRequest request,
            HttpServletRequest httpServletRequest
    ) {
        var tokens = twoFAService.verifyTwoFactor(request);
        AuthTokenResponse response = buildAuthTokenResponse(tokens);
        ResponseCookie refreshCookie = buildRefreshCookie(tokens.refreshToken(), refreshExpirationMs);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success("Two-factor code verified successfully", response, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<TwoFAStatusResponse>> getStatus(
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        CustomUserDetails principal = extractPrincipal(authentication);
        TwoFAStatusResponse response = twoFAService.getTwoFactorStatus(principal);

        return ResponseEntity.ok(ApiResponse.success(
                "Two-factor status loaded successfully",
                response,
                httpServletRequest.getRequestURI()
        ));
    }

    @DeleteMapping("/disable")
    public ResponseEntity<ApiResponse<String>> disableTwoFactor(
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        CustomUserDetails principal = extractPrincipal(authentication);
        twoFAService.disableTwoFactor(principal);

        return ResponseEntity.ok(ApiResponse.success(
                "Two-factor authentication disabled successfully",
                httpServletRequest.getRequestURI()
        ));
    }

    private CustomUserDetails extractPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new UnauthorizedException("Invalid authentication principal");
        }
                return principal;
    }

        private AuthTokenResponse buildAuthTokenResponse(com.java.file_storage_system.service.AuthService.AuthTokens tokens) {
                UserRole role = UserRole.fromString(tokens.role());
                return AuthTokenResponse.builder()
                                .accessToken(tokens.accessToken())
                                .tokenType("Bearer")
                                .expiresInMs(tokens.accessTokenExpiresInMs())
                                .role(tokens.role())
                                .tenantId(tokens.tenantId())
                                .userId(tokens.userId())
                                .username(tokens.username())
                                .email(tokens.email())
                                .redirectUrl(rolePermissionService.getRedirectUrlByRole(role))
                                .userDisplayName(tokens.username())
                                .build();
        }

        private ResponseCookie buildRefreshCookie(String value, long maxAgeMs) {
                return ResponseCookie.from(refreshCookieName, value)
                                .httpOnly(true)
                                .secure(refreshCookieSecure)
                                .path("/api/v1/auth")
                                .maxAge(Duration.ofMillis(Math.max(maxAgeMs, 0L)))
                                .sameSite(refreshCookieSameSite)
                                .build();
        }
}
