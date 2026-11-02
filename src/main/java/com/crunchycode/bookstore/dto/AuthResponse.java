package com.crunchycode.bookstore.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String role,
        long expiresInMs
) {
    public AuthResponse(String token, String username, String role, long expiresInMs) {
        this(token, "Bearer", username, role, expiresInMs);
    }
}
