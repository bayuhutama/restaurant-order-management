package com.restaurant.dto.order;

import com.restaurant.model.enums.PaymentMethod;
import com.restaurant.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String transactionId,
        LocalDateTime paidAt
) {}
