package com.restaurant.dto.menu;

import java.math.BigDecimal;

public record MenuItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        CategoryResponse category,
        boolean available
) {}
