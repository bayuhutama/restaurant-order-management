package com.restaurant.dto.order;

import java.math.BigDecimal;

/**
 * Read-only projection of a single order line item.
 * subtotal = unitPrice × quantity, computed in OrderService.mapToResponse().
 */
public record OrderItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        String menuItemImage,
        int quantity,
        BigDecimal unitPrice,
        /** Pre-calculated line total (unitPrice × quantity). */
        BigDecimal subtotal,
        String notes
) {}
