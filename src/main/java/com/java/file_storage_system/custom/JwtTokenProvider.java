package com.java.file_storage_system.custom;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    @Value("${app.security.jwt-secret:replace-this-with-a-very-long-secret-key}")
    private String jwtSecret;

    @Value("${app.security.jwt-access-expiration-ms:900000}")
    @Getter
    private long accessTokenExpirationMs;

    @Value("${app.security.jwt-refresh-expiration-ms:2592000000}")
    private long refreshTokenExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(CustomUserDetails userDetails) {
        return buildToken(userDetails, ACCESS_TOKEN_TYPE, accessTokenExpirationMs);
    }

    public String generateRefreshToken(CustomUserDetails userDetails) {
        return buildToken(userDetails, REFRESH_TOKEN_TYPE, refreshTokenExpirationMs);
    }


    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateAccessToken(String authToken) {
        return validateTokenByType(authToken, ACCESS_TOKEN_TYPE);
    }

    public boolean validateRefreshToken(String authToken) {
        return validateTokenByType(authToken, REFRESH_TOKEN_TYPE);
    }

    private String buildToken(CustomUserDetails userDetails, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(System.currentTimeMillis() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claims(Map.of(
                        "uid", userDetails.getId(),
                        "role", userDetails.getRole(),
                        "tenantId", userDetails.getTenantId() == null ? "" : userDetails.getTenantId(),
                        TOKEN_TYPE_CLAIM, tokenType
                ))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    private boolean validateTokenByType(String authToken, String expectedType) {
        try {
            String tokenType = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(authToken)
                    .getPayload()
                    .get(TOKEN_TYPE_CLAIM, String.class);

            return expectedType.equals(tokenType);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
