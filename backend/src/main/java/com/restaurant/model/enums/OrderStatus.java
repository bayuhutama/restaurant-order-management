package com.restaurant.model.enums;

/**
 * Lifecycle states of a dine-in order.
 *
 * Active flow: PENDING → CONFIRMED → PREPARING → READY → DELIVERED
 *
 * - AWAITING_PAYMENT: legacy status, no longer used in the active flow
 * - PENDING: order placed; staff notified; waiting for payment confirmation
 * - CONFIRMED: payment received; kitchen acknowledged the order
 * - PREPARING: kitchen is actively cooking
 * - READY: food is ready at the pass; waiting to be served
 * - DELIVERED: food brought to the table; order complete
 * - CANCELLED: order cancelled (admin only after initial placement)
 *
 * Transition rules are enforced in OrderService.VALID_TRANSITIONS.
 * Staff cannot move PENDING → CONFIRMED until payment.status == PAID.
 */
public enum OrderStatus {
    AWAITING_PAYMENT, PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED
}
