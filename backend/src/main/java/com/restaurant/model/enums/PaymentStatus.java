package com.restaurant.model.enums;

/**
 * Lifecycle states of a payment record.
 * - PENDING: payment created but not yet confirmed by the customer
 * - PAID: customer confirmed payment; staff can now advance the order
 * - FAILED: payment attempt failed
 * - REFUNDED: payment was reversed after being PAID
 */
public enum PaymentStatus {
    PENDING, PAID, FAILED, REFUNDED
}
