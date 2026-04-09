package com.restaurant.service;

import com.restaurant.dto.auth.AuthResponse;
import com.restaurant.dto.auth.LoginRequest;
import com.restaurant.dto.auth.RegisterRequest;
import com.restaurant.model.User;
import com.restaurant.model.enums.Role;
import com.restaurant.repository.UserRepository;
import com.restaurant.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles user registration and login.
 * All self-registered users are assigned the CUSTOMER role.
 * STAFF and ADMIN accounts are seeded by DataInitializer or created manually.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Creates a new CUSTOMER account and immediately returns a JWT token.
     * Throws if the username or email is already taken.
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())
                || userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed — username or email already in use: username={}", request.username());
            throw new RuntimeException("Registration failed: username or email already in use");
        }

        User user = User.builder()
                .name(request.name())
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(Role.CUSTOMER)
                .build();

        user = userRepository.save(user);
        log.info("User registered: username={}, id={}", user.getUsername(), user.getId());

        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getName(), user.getUsername(), user.getRole().name());
    }

    /**
     * Authenticates with username + password via Spring Security's AuthenticationManager.
     * Throws BadCredentialsException (→ 401) if credentials are wrong.
     */
    public AuthResponse login(LoginRequest request) {
        // Delegates to DaoAuthenticationProvider — throws BadCredentialsException on failure
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User logged in: username={}, role={}", user.getUsername(), user.getRole());

        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getName(), user.getUsername(), user.getRole().name());
    }
}
