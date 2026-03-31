package com.restaurant.dto.order;

import com.restaurant.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull OrderStatus status) {}
