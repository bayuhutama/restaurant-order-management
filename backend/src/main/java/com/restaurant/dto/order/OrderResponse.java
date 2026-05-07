package com.restaurant.dto.order;

import com.restaurant.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full read-only projection of an Order returned to API clients.
 * customerName/Phone/Email resolve to the registered user's details if
 * available, or fall back to the guest fields stored on the order.
 */
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
) {
    /** Helper to create a copy of this response with a populated payment token. */
    public OrderResponse withPayment(PaymentResponse newPayment) {
        return new OrderResponse(
                id, orderNumber, customerName, customerPhone, customerEmail,
                status, totalAmount, notes, tableNumber, items,
                newPayment, createdAt, updatedAt
        );
    }
}

