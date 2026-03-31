package com.restaurant.dto.order;

import com.restaurant.model.enums.PaymentMethod;
import com.restaurant.model.enums.TableSessionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TableSessionResponse(
        Long id,
        String tableNumber,
        TableSessionStatus status,
        BigDecimal totalAmount,
        int orderCount,
        LocalDateTime openedAt,
        LocalDateTime lastActivityAt,
        PaymentMethod paymentMethod,
        List<OrderResponse> orders
) {}
