package com.restaurant.mapper;

import com.restaurant.dto.order.OrderItemResponse;
import com.restaurant.dto.order.OrderResponse;
import com.restaurant.dto.order.PaymentResponse;
import com.restaurant.model.Order;
import com.restaurant.model.OrderItem;
import com.restaurant.model.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    /**
     * Maps an Order entity to its response DTO.
     * Resolves customer identity: prefers User fields, falls back to guest fields.
     */
    public OrderResponse mapToResponse(Order order) {
        List<OrderItem> items = order.getItems() != null ? order.getItems() : List.of();

        List<OrderItemResponse> itemResponses = items.stream().map(item ->
                new OrderItemResponse(
                        item.getId(),
                        item.getMenuItem().getId(),
                        item.getMenuItem().getName(),
                        item.getMenuItem().getImageUrl(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                        item.getNotes()
                )
        ).toList();

        PaymentResponse paymentResponse = null;
        if (order.getPayment() != null) {
            Payment p = order.getPayment();
            paymentResponse = new PaymentResponse(p.getId(), p.getMethod(), p.getStatus(),
                    p.getAmount(), p.getTransactionId(), p.getPaidAt(), null);
        }

        String customerName  = order.getUser() != null ? order.getUser().getName()  : order.getGuestName();
        String customerPhone = order.getUser() != null ? order.getUser().getPhone() : order.getGuestPhone();
        String customerEmail = order.getUser() != null ? order.getUser().getEmail() : order.getGuestEmail();

        return new OrderResponse(
                order.getId(), order.getOrderNumber(),
                customerName, customerPhone, customerEmail,
                order.getStatus(), order.getTotalAmount(),
                order.getNotes(), order.getTableNumber(),
                itemResponses, paymentResponse,
                order.getCreatedAt(), order.getUpdatedAt()
        );
    }
}
