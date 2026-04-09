package com.restaurant.dto.menu;

import java.math.BigDecimal;

/**
 * Read-only projection of a MenuItem returned to API clients.
 * The nested category field is null if the item has no category assigned.
 */
public record MenuItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        CategoryResponse category,
        boolean available
) {}
