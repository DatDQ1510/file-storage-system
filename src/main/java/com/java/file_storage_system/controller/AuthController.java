package com.java.file_storage_system.controller;

import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.dto.auth.AuthTokenResponse;
import com.java.file_storage_system.dto.auth.ForgotPasswordResetRequest;
import com.java.file_storage_system.dto.auth.ForgotPasswordSendCodeRequest;
import com.java.file_storage_system.dto.auth.ForgotPasswordVerifyCodeRequest;
import com.java.file_storage_system.dto.auth.LoginRequest;
import com.java.file_storage_system.dto.user.changePassword.ChangePasswordRequest;
import com.java.file_storage_system.exception.UnauthorizedException;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${app.security.refresh-cookie-name:refresh_token}")
    private String refreshCookieName;

    @Value("${app.security.jwt-refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.security.refresh-cookie-same-site:Lax}")
    private String refreshCookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        AuthService.AuthTokens tokens = authService.login(request);

        ResponseCookie refreshCookie = buildRefreshCookie(tokens.refreshToken(), refreshExpirationMs);
        AuthTokenResponse response = new AuthTokenResponse(
                tokens.accessToken(),
                "Bearer",
                tokens.accessTokenExpiresInMs(),
                tokens.role(),
                tokens.tenantId()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success("Login successfully", response, httpServletRequest.getRequestURI()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @CookieValue(value = "${app.security.refresh-cookie-name:refresh_token}", required = false) String refreshToken,
            HttpServletRequest httpServletRequest
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Missing refresh token cookie");
        }

        AuthService.AuthTokens tokens = authService.refresh(refreshToken);
        ResponseCookie refreshCookie = buildRefreshCookie(tokens.refreshToken(), refreshExpirationMs);

        AuthTokenResponse response = new AuthTokenResponse(
                tokens.accessToken(),
                "Bearer",
                tokens.accessTokenExpiresInMs(),
                tokens.role(),
                tokens.tenantId()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success("Refresh token successfully", response, httpServletRequest.getRequestURI()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest httpServletRequest) {
        ResponseCookie clearCookie = buildRefreshCookie("", 0L);

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(ApiResponse.success("Logout successfully", httpServletRequest.getRequestURI()));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpServletRequest
    ) {
        CustomUserDetails principal = extractPrincipal(authentication);
        authService.changePassword(principal, request);

        return ResponseEntity.ok(
                ApiResponse.success("Change password successfully", httpServletRequest.getRequestURI())
        );
    }

    @PostMapping("/forgot-password/send-code")
    public ResponseEntity<ApiResponse<String>> sendForgotPasswordCode(
            @Valid @RequestBody ForgotPasswordSendCodeRequest request,
            HttpServletRequest httpServletRequest
    ) {
        authService.sendForgotPasswordCode(request);
        return ResponseEntity.ok(
                ApiResponse.success("Verification code sent successfully", httpServletRequest.getRequestURI())
        );
    }

    @PostMapping("/forgot-password/verify-code")
    public ResponseEntity<ApiResponse<String>> verifyForgotPasswordCode(
            @Valid @RequestBody ForgotPasswordVerifyCodeRequest request,
            HttpServletRequest httpServletRequest
    ) {
        authService.verifyForgotPasswordCode(request);
        return ResponseEntity.ok(
                ApiResponse.success("Verification code is valid", httpServletRequest.getRequestURI())
        );
    }

    @PatchMapping("/forgot-password/reset-password")
    public ResponseEntity<ApiResponse<String>> resetForgotPassword(
            @Valid @RequestBody ForgotPasswordResetRequest request,
            HttpServletRequest httpServletRequest
    ) {
        authService.resetForgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("Reset password successfully", httpServletRequest.getRequestURI())
        );
    }

        private CustomUserDetails extractPrincipal(Authentication authentication) {
                if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
                        throw new UnauthorizedException("Invalid authentication principal");
                }
                return principal;
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
