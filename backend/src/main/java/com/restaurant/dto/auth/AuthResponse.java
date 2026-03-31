package com.restaurant.dto.auth;

public record AuthResponse(
        String token,
        String type,
        Long id,
        String name,
        String username,
        String role
) {
    public AuthResponse(String token, Long id, String name, String username, String role) {
        this(token, "Bearer", id, name, username, role);
    }
}
