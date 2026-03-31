package com.restaurant.dto.order;

import com.restaurant.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        String customerName,
        String customerPhone,
        String customerEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        String notes,
        String tableNumber,
        List<OrderItemResponse> items,
        PaymentResponse payment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
