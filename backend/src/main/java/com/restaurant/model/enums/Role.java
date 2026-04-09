package com.restaurant.model.enums;

/**
 * User roles in the system.
 * - CUSTOMER: registered dine-in guest who can place orders
 * - STAFF: restaurant employee who manages orders from the dashboard
 * - ADMIN: full access — manages menu, categories, users, and orders
 */
public enum Role {
    CUSTOMER, STAFF, ADMIN
}
