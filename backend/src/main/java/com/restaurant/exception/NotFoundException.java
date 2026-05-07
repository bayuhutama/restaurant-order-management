package com.restaurant.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested entity (order, menu item, category, etc.) does not exist.
 * Automatically maps to HTTP 404 Not Found via {@link BusinessException#getStatus()}.
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
