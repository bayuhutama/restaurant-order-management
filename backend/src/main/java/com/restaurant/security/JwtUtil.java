package com.restaurant.security;

import com.restaurant.model.User;
import com.restaurant.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import org.springframework.beans.factory.annotation.Value;
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
 * - issuedAt: current timestamp
 * - expiration: issuedAt + role-based lifetime (see {@link #lifetimeFor})
 *
 * Token lifetime is role-based:
 * - STAFF / ADMIN: long lifetime so a whole shift (e.g. 10AM–10PM) fits in one
 *   login. Configured via {@code jwt.expiration.staff} (default 13 hours).
 * - CUSTOMER (and any unauthenticated self-registered user): short lifetime
 *   configured via {@code jwt.expiration} (default 8 hours).
 *
 * The signing key is derived from the jwt.secret property using HMAC-SHA256.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /** Default token lifetime in milliseconds — used for customers. */
    @Value("${jwt.expiration}")
    private long defaultExpiration;

    /** Longer token lifetime in milliseconds — used for staff and admin shifts. */
    @Value("${jwt.expiration.staff:46800000}")
    private long staffExpiration;

    private static final MacAlgorithm ALGORITHM = Jwts.SIG.HS256;

    /** Derives the signing key from the configured secret string. */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns the token lifetime in milliseconds appropriate for the given role. */
    private long lifetimeFor(Role role) {
        return (role == Role.STAFF || role == Role.ADMIN) ? staffExpiration : defaultExpiration;
    }

    /**
     * Creates a signed JWT for a concrete {@link User}.
     * Preferred entry point — lets the token lifetime vary by role.
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + lifetimeFor(user.getRole())))
                .signWith(getSigningKey(), ALGORITHM)
                .compact();
    }

    /**
     * Creates a signed JWT using the default (customer) lifetime.
     * Kept for callers that only hold a Spring Security {@link UserDetails}.
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + defaultExpiration))
                .signWith(getSigningKey(), ALGORITHM)
                .compact();
    }

    /** Extracts the username (subject) from a JWT without validating its expiry. */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Returns true if the token's subject matches the given user and the token is not expired.
     * Called by JwtAuthenticationFilter on every authenticated request.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    /**
     * Parses and verifies the token signature, returning all claims.
     * Throws on invalid or expired tokens.
     * verifyWith() constrains the algorithm to the key type (HMAC-SHA), so a
     * separate requireAlgorithm() call is unnecessary in jjwt 0.12.x.
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
