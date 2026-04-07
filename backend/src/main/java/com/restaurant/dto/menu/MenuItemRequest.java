package com.restaurant.dto.menu;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999999.99") BigDecimal price,
        @Pattern(regexp = "^(https?://.{1,490})?$", message = "Image URL must start with http:// or https://")
        @Size(max = 500) String imageUrl,
        Long categoryId,
        boolean available
) {}
