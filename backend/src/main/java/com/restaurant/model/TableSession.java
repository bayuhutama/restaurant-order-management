package com.restaurant.model;

import com.restaurant.model.enums.PaymentMethod;
import com.restaurant.model.enums.TableSessionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Groups all orders placed at the same table during a single visit.
 *
 * A new session is opened automatically when the first order arrives for a
 * table that has no OPEN session. Multiple customers sitting at the same table
 * can each place separate orders; all are linked to the same session.
 *
 * Session lifecycle:
 *  OPEN → PAID (customer settles via /table-sessions/{tableNumber}/pay)
 *  OPEN → EXPIRED (staff closes manually, or inactivity timeout triggers @Scheduled expiry)
 */
@Entity
@Table(name = "table_sessions", indexes = {
        // Composite index covers the hot path: find the OPEN session for a given table
        @Index(name = "idx_table_sessions_table_number_status", columnList = "table_number, status"),
        // status alone is used by the scheduler and getOpenSessions()
        @Index(name = "idx_table_sessions_status", columnList = "status")
})
@Getter
@Setter
@ToString(exclude = {"orders"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifies the physical table (matches the value encoded in the QR code). */
    @Column(name = "table_number", nullable = false)
    private String tableNumber;

    /** Current lifecycle state of the session. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableSessionStatus status;

    /** When the session was first created (set automatically in @PrePersist). */
    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    /**
     * Updated every time a new order is added to this session.
     * Used by the expiry scheduler to detect inactive tables.
     */
    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    /** Set when the session transitions to PAID or EXPIRED. */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** Payment method chosen when the whole table's bill is settled. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    /** All orders placed during this session; loaded lazily. */
    @OneToMany(mappedBy = "tableSession", fetch = FetchType.LAZY)
    private List<Order> orders;

    /** Initialises timestamps and status automatically on first persist. */
    @PrePersist
    protected void onCreate() {
        openedAt = LocalDateTime.now();
        lastActivityAt = LocalDateTime.now();
        status = TableSessionStatus.OPEN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableSession that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

