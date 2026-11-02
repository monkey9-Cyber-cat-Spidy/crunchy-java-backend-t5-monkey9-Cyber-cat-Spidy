package com.crunchycode.bookstore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service generating and validating JSON Web Tokens (JJWT 0.12+).
 * Enforces finite token expiration (never infinite).
 * Relies on externalized secret keys without committing secrets.
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // Enforce strong 256-bit key
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs)) // Strictly finite expiration
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            Claims claims = extractClaims(token);
            boolean isExpired = claims.getExpiration().before(new Date());
            return !isExpired && claims.getSubject().equals(expectedUsername);
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
