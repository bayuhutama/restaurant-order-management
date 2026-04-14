package com.restaurant.model.enums;

/**
 * User roles in the system.
 * - STAFF: restaurant employee who manages orders from the dashboard
 * - ADMIN: full access — manages menu, categories, users, and orders
 *
 * Customers do not have accounts; they order as guests via QR-code flow.
 */
public enum Role {
    STAFF, ADMIN
}
