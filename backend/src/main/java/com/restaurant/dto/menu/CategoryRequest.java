package com.restaurant.dto.menu;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String name,
        String description,
        String imageUrl
) {}
