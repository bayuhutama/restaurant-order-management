package com.restaurant.dto.order;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/orders/{orderNumber}/pay.
 * The paymentToken was issued to the customer in the placeOrder() response
 * and is required to prevent unauthenticated third parties from confirming
 * payment for an order they do not own.
 */
public record PaymentConfirmRequest(
        @NotBlank String paymentToken
) {}
