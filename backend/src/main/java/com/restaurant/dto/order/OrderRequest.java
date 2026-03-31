package com.restaurant.dto.order;

import com.restaurant.model.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        String notes,
        @NotBlank String tableNumber,
        String guestName,
        String guestPhone,
        String guestEmail,
        @NotNull PaymentMethod paymentMethod
) {}
