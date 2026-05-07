package com.restaurant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralised exception handler for all REST controllers.
 * Converts exceptions into structured JSON error responses so the frontend
 * always receives a consistent { "message": "..." } shape.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Known business-rule messages that are safe to expose to clients.
     * Any RuntimeException whose message does NOT start with one of these prefixes
     * is treated as an unexpected internal error and masked with a generic message.
     */
    /**
     * Handles expected business rule violations.
     * The exception itself dictates the appropriate HTTP status code (default 400 Bad Request).
     * The message is safe to display to the user.
     */
    @ExceptionHandler(com.restaurant.exception.BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(com.restaurant.exception.BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Catches all unhandled RuntimeExceptions.
     * These are treated as unexpected internal errors and masked with a generic message,
     * preventing internal details (SQL errors, class names, etc.) from leaking.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An unexpected error occurred"));
    }

    /**
     * Handles failed login attempts — Spring Security throws this when credentials are wrong.
     * Returns a generic 401 message to avoid username enumeration.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Failed login attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid email or password"));
    }

    /**
     * Handles @PreAuthorize and role-check failures.
     * Returns 403 Forbidden with a generic message.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access denied"));
    }

    /**
     * Handles path variable / query parameter type conversion failures.
     * For example, requesting /api/menu/abc when the endpoint expects a Long id.
     * Returns 400 Bad Request with a user-friendly message instead of leaking
     * the internal class cast details.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch: parameter '{}' could not be converted to {}", ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid parameter: " + ex.getName()));
    }

    /**
     * Handles @Valid constraint violations on request bodies.
     * Returns 400 Bad Request with a map of field → error message pairs,
     * so the frontend can highlight specific form fields.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Validation failed", "errors", errors));
    }
}
