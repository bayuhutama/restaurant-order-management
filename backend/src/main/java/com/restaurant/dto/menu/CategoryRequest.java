package com.restaurant.dto.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Pattern(regexp = "^(https?://.{1,490})?$", message = "Image URL must start with http:// or https://")
        @Size(max = 500) String imageUrl
) {}
