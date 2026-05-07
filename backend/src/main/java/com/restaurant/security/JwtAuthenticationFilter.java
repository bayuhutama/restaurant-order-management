package com.restaurant.security;

import com.restaurant.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Servlet filter that runs once per request and sets up Spring Security authentication
 * from a JWT token in the Authorization header.
 *
 * Flow:
 * 1. Extract the "Bearer <token>" value from the Authorization header.
 * 2. Parse the username from the token.
 * 3. Load the user from the database.
 * 4. Validate the token (signature + expiry).
 * 5. If valid, set the authentication in the SecurityContext.
 *
 * If the header is missing, malformed, or the token is invalid, the filter
 * passes the request through without setting authentication (the request will
 * fail authorization downstream if the endpoint requires it).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Skip if no Bearer token is present — public endpoints will still work
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Strip "Bearer " prefix to get the raw token
        final String token = authHeader.substring(7);

        try {
            // Parse and verify token once
            io.jsonwebtoken.Claims claims = jwtUtil.parseToken(token);
            final String username = claims.getSubject();

            // Only authenticate if not already authenticated (avoids redundant DB calls)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Single-session enforcement
                boolean versionOk = true;
                if (userDetails instanceof User user) {
                    Object v = claims.get(JwtUtil.CLAIM_TOKEN_VERSION);
                    Long claimVersion = (v instanceof Number n) ? n.longValue() : null;
                    versionOk = Objects.equals(claimVersion, user.getTokenVersion());
                    if (!versionOk) {
                        log.warn("Rejecting JWT for user={} — tokenVersion mismatch (claim={}, current={})",
                                username, claimVersion, user.getTokenVersion());
                    }
                }

                // Check username matches and expiry
                boolean valid = username.equals(userDetails.getUsername()) && !claims.getExpiration().before(new java.util.Date());

                if (versionOk && valid) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // Log the issue but let the request continue — downstream auth will reject it if needed
            log.warn("Invalid JWT token: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
