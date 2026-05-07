package com.restaurant.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One line item inside an Order — captures a specific menu item,
 * the quantity ordered, and the unit price at the time of ordering.
 *
 * Unit price is snapshotted from MenuItem.price so order history
 * is unaffected by future menu price changes.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@ToString(exclude = {"order"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent order; loaded lazily to avoid pulling the full order graph unnecessarily. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** The menu item ordered; loaded eagerly because its name/image are always needed for display. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    /** Number of units ordered (1–100, validated in OrderItemRequest). */
    @Column(nullable = false)
    private int quantity;

    /** Price per unit snapshotted from the menu item at order time. */
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    /** Optional customer notes for this item (e.g. "no onions"). */
    private String notes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

