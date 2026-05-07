package com.restaurant.model;

import com.restaurant.model.enums.PaymentMethod;
import com.restaurant.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment record associated 1:1 with an Order.
 *
 * Flow:
 * 1. Created with status PENDING when the order is placed.
 * 2. Customer confirms payment on /payment/:orderNumber → status becomes PAID.
 * 3. Staff can then advance the order from PENDING → CONFIRMED.
 *
 * For CASH orders, payment is auto-marked PAID when the order is DELIVERED.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@ToString(exclude = {"order"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The order this payment belongs to. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** How the customer chose to pay (CASH or CARD). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    /** Current payment lifecycle state. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /** Total amount due, copied from the order total. */
    @Column(nullable = false)
    private BigDecimal amount;

    /**
     * One-time cryptographic token generated when the order is placed.
     * Must be supplied by the customer when calling POST /api/orders/{orderNumber}/pay.
     * This prevents an unauthenticated third party who learns the order number (e.g. by
     * watching the public /topic/orders WebSocket) from marking a stranger's order as PAID.
     *
     * Nullable to preserve backward compatibility with payments created before this
     * field was introduced — those rows are exempt from token validation.
     */
    @Column(name = "payment_token", unique = true)
    private String paymentToken;

    /** System-generated transaction ID set when the payment is confirmed (e.g. TXN-XXXXXXXX). */
    @Column(name = "transaction_id")
    private String transactionId;

    /** Timestamp when the payment was confirmed (null until status = PAID). */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Timestamp set automatically when the payment record is first created. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

