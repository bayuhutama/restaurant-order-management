package com.restaurant.dto.order;

import com.restaurant.model.enums.PaymentMethod;
import com.restaurant.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only projection of a Payment record, embedded inside OrderResponse.
 * transactionId and paidAt are null until the payment is confirmed.
 *
 * paymentToken is the one-time token required to call POST /api/orders/{n}/pay.
 * It is NON-NULL only in the response to the initial placeOrder() call so the
 * customer can store it for the payment page. In all other contexts (WebSocket
 * broadcasts, staff views, order-tracking reads) it is deliberately null to
 * prevent token harvest from the public WebSocket.
 */
public record PaymentResponse(
        Long id,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        /** System-generated ID set when the payment transitions to PAID. */
        String transactionId,
        /** Timestamp when payment was confirmed; null if still PENDING. */
        LocalDateTime paidAt,
        /** One-time token; non-null only in the placeOrder response. Null everywhere else. */
        String paymentToken
) {}
