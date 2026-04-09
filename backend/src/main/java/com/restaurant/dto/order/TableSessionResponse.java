package com.restaurant.dto.order;

import com.restaurant.model.enums.PaymentMethod;
import com.restaurant.model.enums.TableSessionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only projection of a TableSession returned to staff and customers.
 * totalAmount is the sum of all order totals within the session.
 * orderCount is the number of individual orders placed at the table.
 */
public record TableSessionResponse(
        Long id,
        String tableNumber,
        TableSessionStatus status,
        /** Sum of all order totals for this table visit. */
        BigDecimal totalAmount,
        /** Number of separate orders placed during this session. */
        int orderCount,
        LocalDateTime openedAt,
        LocalDateTime lastActivityAt,
        /** Set when the session is paid; null otherwise. */
        PaymentMethod paymentMethod,
        List<OrderResponse> orders
) {}
