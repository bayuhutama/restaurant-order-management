package com.restaurant.service;

import com.restaurant.dto.auth.AuthResponse;
import com.restaurant.dto.auth.LoginRequest;
import com.restaurant.model.User;
import com.restaurant.repository.UserRepository;
import com.restaurant.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Handles staff/admin login.
 * Only STAFF and ADMIN accounts authenticate — provisioned by DataInitializer
 * or created manually. Customers never log in; they order as guests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Authenticates with username + password via Spring Security's AuthenticationManager.
     * Throws BadCredentialsException (→ 401) if credentials are wrong.
     *
     * On success, bumps {@code User.tokenVersion} and saves the user before
     * issuing the JWT. The new token carries the incremented version as a claim,
     * so any previously issued token for this user is invalidated — this is the
     * single-active-session-per-user guarantee.
     */
    public AuthResponse login(LoginRequest request) {
        // Delegates to DaoAuthenticationProvider — throws BadCredentialsException on failure
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long current = user.getTokenVersion();
        user.setTokenVersion((current == null ? 0L : current) + 1L);
        user = userRepository.save(user);

        log.info("User logged in: username={}, role={}, tokenVersion={}",
                user.getUsername(), user.getRole(), user.getTokenVersion());

        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getName(), user.getUsername(), user.getRole().name());
    }
}
