package com.restaurant.dto.order;

import com.restaurant.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/table-sessions/{tableNumber}/pay.
 * The chosen payment method is recorded on the session and applied to
 * all pending payment records within it.
 */
public record PaySessionRequest(@NotNull PaymentMethod paymentMethod) {}
