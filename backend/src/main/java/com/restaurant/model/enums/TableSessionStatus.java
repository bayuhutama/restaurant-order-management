package com.restaurant.model.enums;

/**
 * Status of a table session that groups all orders from one table visit.
 * - OPEN: table is in use; new orders can still be added
 * - PAID: all orders settled; session closed via payment
 * - EXPIRED: session timed out due to inactivity, or staff manually closed it
 */
public enum TableSessionStatus { OPEN, PAID, EXPIRED }
