package com.restaurant.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderItemRequest(
        @NotNull Long menuItemId,
        @Min(1) @Max(100) int quantity,
        @Size(max = 200) String notes
) {}
