package com.restaurant.dto.auth;

/**
 * Returned to the client after a successful login or registration.
 * Contains the JWT token and the logged-in user's basic profile.
 */
public record AuthResponse(
        /** The JWT bearer token the client should include in subsequent requests. */
        String token,
        /** Always "Bearer" — the HTTP authentication scheme. */
        String type,
        Long id,
        String name,
        String username,
        /** The user's role string (e.g. "ADMIN", "STAFF", "CUSTOMER"). */
        String role
) {
    /** Convenience constructor that hard-codes the token type as "Bearer". */
    public AuthResponse(String token, Long id, String name, String username, String role) {
        this(token, "Bearer", id, name, username, role);
    }
}
