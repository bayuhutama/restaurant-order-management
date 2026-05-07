package com.restaurant.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single item on the restaurant menu.
 * Items belong to a Category and can be toggled available/unavailable by admins.
 * Prices are stored in Indonesian Rupiah (IDR) as BigDecimal for precision.
 */
@Entity
@Table(name = "menu_items")
@Getter
@Setter
@ToString(exclude = {"category"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Name displayed on the menu card. */
    @Column(nullable = false)
    private String name;

    /** Optional description; shown with a hover tooltip on truncated cards. */
    @Column(length = 1000)
    private String description;

    /** Price in IDR; validated to be between 0.01 and 99,999,999.99. */
    @Column(nullable = false)
    private BigDecimal price;

    /** URL of the item's photo (external URL or uploaded path via FileUploadService). */
    @Column(name = "image_url")
    private String imageUrl;

    /** The category this item belongs to; loaded lazily to avoid N+1 queries. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Whether the item is currently orderable.
     * Unavailable items are hidden from the customer menu but retained in the database.
     */
    @Column(nullable = false)
    private boolean available = true;

    /** Timestamp set automatically when the item is first persisted. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MenuItem that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

