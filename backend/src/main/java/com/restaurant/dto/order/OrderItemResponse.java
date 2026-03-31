package com.restaurant.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        String menuItemImage,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String notes
) {}
