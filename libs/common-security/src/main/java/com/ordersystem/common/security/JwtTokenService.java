package com.ordersystem.common.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public final class JwtTokenService {

    private static final int MIN_SECRET_LENGTH_BYTES = 32; // 256 bits for HS256

    private final Key signingKey;
    private final Duration tokenValidity;

    public JwtTokenService(String secret, Duration tokenValidity) {
        validateSecret(secret);

        // FIXED: Use UTF-8 charset explicitly to prevent platform-dependent encoding issues
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.tokenValidity = tokenValidity;
    }

    /**
     * Validates that the JWT secret meets minimum security requirements.
     *
     * @param secret the secret to validate
     * @throws IllegalStateException if secret is null, blank, or too short
     */
    private void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException(
                String.format("JWT secret must be at least %d bytes (current: %d bytes). " +
                             "Use a strong secret with minimum 32 characters.",
                             MIN_SECRET_LENGTH_BYTES, secretBytes.length)
            );
        }
    }

    public String createToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(tokenValidity)))
            .addClaims(claims)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public io.jsonwebtoken.Claims getClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public Key getSigningKey() {
        return signingKey;
    }
}
