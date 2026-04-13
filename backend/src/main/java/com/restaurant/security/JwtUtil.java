package com.restaurant.security;

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
 * - expiration: issuedAt + jwt.expiration (milliseconds, from application.properties)
 *
 * The signing key is derived from the jwt.secret property using HMAC-SHA256.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /** Token validity duration in milliseconds (configured via jwt.expiration). */
    @Value("${jwt.expiration}")
    private long expiration;

    private static final MacAlgorithm ALGORITHM = Jwts.SIG.HS256;

    /** Derives the signing key from the configured secret string. */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Creates a signed JWT with the user's username as the subject. */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
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
