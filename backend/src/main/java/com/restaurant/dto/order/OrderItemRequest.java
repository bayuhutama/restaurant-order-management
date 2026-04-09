package com.restaurant.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One line item in an order placement request.
 * Quantity is limited to 1–100 per item per order.
 */
public record OrderItemRequest(
        /** ID of the menu item to order. */
        @NotNull Long menuItemId,
        @Min(1) @Max(100) int quantity,
        /** Optional per-item note (e.g. "extra spicy", "no onions"). */
        @Size(max = 200) String notes
) {}
