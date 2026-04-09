package com.restaurant.dto.menu;

/**
 * Read-only projection of a Category returned to API clients.
 */
public record CategoryResponse(
        Long id,
        String name,
        String description,
        String imageUrl
) {}
