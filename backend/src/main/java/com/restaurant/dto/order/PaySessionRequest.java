package com.restaurant.dto.order;

import com.restaurant.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaySessionRequest(@NotNull PaymentMethod paymentMethod) {}
