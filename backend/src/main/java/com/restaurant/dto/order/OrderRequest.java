package com.restaurant.dto.order;

import com.restaurant.model.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        @Size(max = 500) String notes,
        @NotBlank @Size(max = 20) String tableNumber,
        @Size(max = 100) String guestName,
        @Size(max = 20) String guestPhone,
        @Email @Size(max = 100) String guestEmail,
        @NotNull PaymentMethod paymentMethod
) {}
