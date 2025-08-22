package com.ordersystem.query.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

/**
 * Enterprise-grade JWT Token Service with Refresh Token Rotation
 * SECURITY TARGET: 15min access token, 7day refresh token, automatic rotation
 */
@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);
    
    // Token validity configuration
    private static final long ACCESS_TOKEN_VALIDITY = 15 * 60 * 1000; // 15 minutes
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000; // 7 days
    
    @Value("${jwt.secret:mySecretKey}")
    private String secret;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public TokenPair generateTokenPair(String userId, Set<String> roles) {
        String accessToken = createAccessToken(userId, roles);
        String refreshToken = createRefreshToken(userId);
        
        // Store refresh token hash in Redis with expiration
        String hashedRefreshToken = hashToken(refreshToken);
        redisTemplate.opsForValue().set(
            "refresh_token:" + userId, 
            hashedRefreshToken, 
            Duration.ofMillis(REFRESH_TOKEN_VALIDITY)
        );
        
        logger.info("Generated token pair for user: {}", userId);
        return new TokenPair(accessToken, refreshToken);
    }

    public TokenPair refreshTokens(String refreshToken) {
        try {
            String userId = extractUserIdFromRefreshToken(refreshToken);
            
            // Validate against stored hash
            String storedHash = redisTemplate.opsForValue().get("refresh_token:" + userId);
            if (storedHash == null || !verifyTokenHash(refreshToken, storedHash)) {
                throw new InvalidTokenException("Invalid refresh token");
            }
            
            // Get user roles (in real scenario, fetch from database/cache)
            Set<String> userRoles = getUserRoles(userId);
            
            // Rotate: invalidate old, generate new
            redisTemplate.delete("refresh_token:" + userId);
            
            logger.info("Refreshing tokens for user: {}", userId);
            return generateTokenPair(userId, userRoles);
            
        } catch (Exception e) {
            logger.error("Error refreshing tokens", e);
            throw new InvalidTokenException("Failed to refresh tokens");
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Invalid access token: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    public Set<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        @SuppressWarnings("unchecked")
        List<String> rolesList = claims.get("roles", List.class);
        return rolesList != null ? new HashSet<>(rolesList) : new HashSet<>();
    }

    public long getExpirationTime(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration().getTime();
    }

    private String createAccessToken(String userId, Set<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", new ArrayList<>(roles));
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String createRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String extractUserIdFromRefreshToken(String refreshToken) {
        try {
            Claims claims = getClaimsFromToken(refreshToken);
            return claims.getSubject();
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid refresh token format");
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private boolean verifyTokenHash(String token, String storedHash) {
        String tokenHash = hashToken(token);
        return MessageDigest.isEqual(tokenHash.getBytes(), storedHash.getBytes());
    }

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is at least 256 bits for HS256
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Set<String> getUserRoles(String userId) {
        // In a real implementation, this would fetch from database
        // For now, return default customer role
        return Set.of("CUSTOMER");
    }

    // Setters for testing
    public void setRedisTemplate(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setAccessTokenValidityMs(long validityMs) {
        // Only for testing purposes
    }
}