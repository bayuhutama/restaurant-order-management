package com.restaurant.controller;

import com.restaurant.dto.auth.AuthResponse;
import com.restaurant.dto.auth.LoginRequest;
import com.restaurant.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles user authentication endpoints.
 * The login route is public (no JWT required). Only staff and admin accounts
 * log in — STAFF and ADMIN users are provisioned by DataInitializer or seeded
 * manually. Customers order as guests and never authenticate.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Authenticates with username + password and returns a JWT token. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
