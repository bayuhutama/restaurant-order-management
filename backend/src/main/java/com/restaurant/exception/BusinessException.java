package com.restaurant.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all business-rule violations that should be returned
 * to the client with a meaningful message and appropriate HTTP status.
 *
 * Subclasses (e.g. {@link NotFoundException}) set a default status, but callers
 * can also construct with an explicit status for one-off cases.
 *
 * Replaces the previous pattern of throwing raw {@link RuntimeException} and
 * relying on string-prefix matching in GlobalExceptionHandler to decide whether
 * the message is safe to expose. Now the handler simply catches BusinessException
 * and uses {@link #getStatus()} directly.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
