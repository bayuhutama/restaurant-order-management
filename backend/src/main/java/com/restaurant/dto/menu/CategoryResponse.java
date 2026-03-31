package com.restaurant.dto.menu;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        String imageUrl
) {}
