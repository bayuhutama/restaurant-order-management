package com.restaurant.dto.order;

import com.restaurant.model.enums.PaymentMethod;
import com.restaurant.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only projection of a Payment record, embedded inside OrderResponse.
 * transactionId and paidAt are null until the payment is confirmed.
 */
public record PaymentResponse(
        Long id,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        /** System-generated ID set when the payment transitions to PAID. */
        String transactionId,
        /** Timestamp when payment was confirmed; null if still PENDING. */
        LocalDateTime paidAt
) {}
