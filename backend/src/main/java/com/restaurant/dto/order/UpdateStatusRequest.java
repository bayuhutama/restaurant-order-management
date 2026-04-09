package com.restaurant.dto.order;

import com.restaurant.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for PATCH /api/staff/orders/{id}/status and
 * PATCH /api/admin/orders/{id}/status.
 * The target status is validated against VALID_TRANSITIONS in OrderService.
 */
public record UpdateStatusRequest(@NotNull OrderStatus status) {}
