package com.digitalqueue.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JwtTokenProvider - Generates, validates, and parses JWT tokens.
 * 
 * JWT Structure:
 * <header>.<payload>.<signature>
 * 
 * Example:
 * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
 * eyJzdWIiOiJqb2huX2RvZSIsInJvbGUiOiJDVVNUT01FUiIsImV4cCI6MTcxNjU0MzIwMH0.
 * kSCWLQBL-wO7u-LgIKgJ3D5P0k2UqJ6f6vLQkYKx5QE
 * 
 * Header: Algorithm (HS256) and type (JWT)
 * Payload: Claims (username, role, expiration)
 * Signature: HMAC-SHA256 hash of header+payload with secret key
 * 
 * @Component: Spring component, instantiated at startup
 * @Value: Injects values from application.properties
 */
@Component
@Slf4j
public class JwtTokenProvider {

    /**
     * Secret key for signing tokens.
     * Injected from application.properties
     * Must be at least 32 characters for HS256
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Token expiration time in milliseconds.
     * Injected from application.properties
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // ==========================================
    // Token Generation
    // ==========================================

    /**
     * Generate JWT token from Authentication object.
     * 
     * Steps:
     * 1. Extract username and roles from Authentication
     * 2. Set claims (username, role, expiration)
     * 3. Sign with HMAC-SHA256
     * 4. Return compact token string
     * 
     * @param authentication: Spring Security Authentication object
     * @return: JWT token string
     */
    public String generateToken(Authentication authentication) {
        log.debug("Generating JWT token for user: {}", authentication.getName());

        String username = authentication.getName();
        
        // Extract roles/authorities
        List<String> roles = authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        String token = Jwts.builder()
            // Subject: who this token is about (username)
            .subject(username)
            // Custom claim: store user roles
            .claim("roles", roles)
            // Issued at: when token was created
            .issuedAt(now)
            // Expiration: when token expires
            .expiration(expiryDate)
            // Sign: use HMAC-SHA256 with secret key
            .signWith(getSigningKey())
            .compact();

        log.debug("JWT token generated successfully for user: {}", username);
        return token;
    }

    /**
     * Generate JWT token from username.
     * Used when issuing tokens without full Authentication object.
     * 
     * @param username: Username for token
     * @param roles: List of user roles
     * @return: JWT token string
     */
    public String generateTokenFromUsername(String username, List<String> roles) {
        log.debug("Generating JWT token for username: {}", username);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        String token = Jwts.builder()
            .subject(username)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();

        return token;
    }

    // ==========================================
    // Token Validation
    // ==========================================

    /**
     * Validate JWT token.
     * 
     * Checks:
     * 1. Signature is valid (not tampered)
     * 2. Token is not expired
     * 3. Payload can be parsed
     * 
     * @param token: JWT token string
     * @return: true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);

            log.debug("JWT token validated successfully");
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // ==========================================
    // Token Parsing
    // ==========================================

    /**
     * Extract username from JWT token.
     * 
     * @param token: JWT token string
     * @return: Username (subject claim)
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Extract all claims (payload) from JWT token.
     * Claims are name-value pairs stored in token payload.
     * 
     * @param token: JWT token string
     * @return: Claims object containing all token data
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Extract expiration time from JWT token.
     * 
     * @param token: JWT token string
     * @return: Expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Get remaining expiration time in seconds.
     * Used by frontend to know when to refresh token.
     * 
     * @param token: JWT token string
     * @return: Seconds until expiration
     */
    public long getExpirationTimeInSeconds(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        Date now = new Date();
        return (expirationDate.getTime() - now.getTime()) / 1000;
    }

    // ==========================================
    // Private Helper Methods
    // ==========================================

    /**
     * Get signing key from secret.
     * 
     * HMAC-SHA256 requires a key derived from the secret string.
     * SecretKey is an interface for cryptographic keys.
     * Keys.hmacShaKeyFor() creates proper SecretKey from bytes.
     * 
     * Why this matters:
     * - Ensures key is proper length and format
     * - Prevents "weak key" exceptions
     * - Compatible with HMAC-SHA256 algorithm
     * 
     * @return: SecretKey for signing/verification
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
