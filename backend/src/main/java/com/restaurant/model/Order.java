package com.restaurant.model;

import com.restaurant.model.TableSession;
import com.restaurant.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A single customer order in the dine-in restaurant system.
 *
 * Orders can be placed by:
 * - A registered User (user field is set; guest fields are null)
 * - A guest (user is null; guestName/guestPhone/guestEmail are used instead)
 *
 * Every order belongs to a TableSession that groups all orders at one table
 * during a visit. The order also denormalises tableNumber for quick lookups.
 *
 * Lifecycle: PENDING → CONFIRMED → PREPARING → READY → DELIVERED
 * See OrderStatus for detailed rules.
 */
@Entity
@Table(name = "orders", indexes = {
        // status is filtered on every staff/admin query — composite with created_at covers sorted filtering
        @Index(name = "idx_orders_status_created_at", columnList = "status, created_at"),
        // table_number is used by the running-bill banner and WebSocket topic routing
        @Index(name = "idx_orders_table_number", columnList = "table_number"),
        // table_session_id FK lookups (findByTableSessionId) need an index for fast joins
        @Index(name = "idx_orders_table_session_id", columnList = "table_session_id")
})
@Getter
@Setter
@ToString(exclude = {"items", "payment", "tableSession", "user"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable unique reference in the format ORD-YYYYMMDD-XXXXXX. */
    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    /** Set for registered users; null for guest orders. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Guest identity fields — used when user is null

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_phone")
    private String guestPhone;

    @Column(name = "guest_email")
    private String guestEmail;

    /** Line items; cascaded so saving the order also saves its items. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    /** Current order lifecycle state. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /** Optional overall notes for the entire order (e.g. "allergy: nuts"). */
    private String notes;

    /** Physical table number this order is associated with. */
    @Column(name = "table_number")
    private String tableNumber;

    /** Pre-calculated sum of all line item subtotals (quantity × unit_price). */
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    /** Associated payment record; cascaded so it is deleted with the order. */
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    /** The table session this order belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_session_id")
    private TableSession tableSession;

    /** Set once on insert. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Updated on every status change. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Identity based on primary key only — safe for managed JPA entities.
     * Avoids triggering lazy-loading of collections (items, payment, etc.)
     * that Lombok's @Data-generated equals/hashCode would include.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

