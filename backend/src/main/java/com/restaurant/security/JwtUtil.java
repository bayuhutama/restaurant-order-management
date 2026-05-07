package com.restaurant.security;

import com.restaurant.model.User;
import com.restaurant.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility for generating and validating JWT tokens using HS256 (HMAC-SHA256).
 *
 * Token structure:
 * - subject: username
 * - tokenVersion: monotonic counter from the User row (single-session enforcement)
 * - issuedAt: current timestamp
 * - expiration: issuedAt + role-based lifetime (see {@link #lifetimeFor})
 *
 * Role-based token lifetime:
 * - ADMIN: short lifetime to reduce the blast radius of a stolen admin token
 *          (jwt.expiration.admin, default 1h).
 * - STAFF: long lifetime so a whole shift fits in one login
 *          (jwt.expiration.staff, default 13h).
 *
 * Single-session enforcement:
 * The tokenVersion claim is compared against the User's current tokenVersion
 * on every authenticated request by JwtAuthenticationFilter. AuthService bumps
 * the counter on each successful login, invalidating any prior tokens.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /** Staff shift-length token lifetime in milliseconds — 13h by default. */
    @Value("${jwt.expiration.staff:46800000}")
    private long staffExpiration;

    /** Short admin token lifetime in milliseconds — 1h by default. */
    @Value("${jwt.expiration.admin:3600000}")
    private long adminExpiration;

    private static final MacAlgorithm ALGORITHM = Jwts.SIG.HS256;

    /** Claim name for the user's current token version — drives single-session enforcement. */
    public static final String CLAIM_TOKEN_VERSION = "tokenVersion";

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns the token lifetime in milliseconds appropriate for the given role. */
    private long lifetimeFor(Role role) {
        return role == Role.ADMIN ? adminExpiration : staffExpiration;
    }

    /**
     * Creates a signed JWT for a concrete {@link User}.
     * Preferred entry point — sets role-based lifetime and embeds tokenVersion.
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + lifetimeFor(user.getRole())))
                .signWith(signingKey, ALGORITHM)
                .compact();
    }

    /**
     * Parses and verifies the token signature, returning all claims.
     * Throws on invalid or expired tokens.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
