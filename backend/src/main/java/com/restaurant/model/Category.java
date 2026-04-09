package com.restaurant.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a menu category (e.g. Appetizers, Main Course, Beverages).
 * Categories group menu items on the customer-facing menu page.
 */
@Entity
@Table(name = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name shown to customers and in the admin panel. */
    @Column(nullable = false)
    private String name;

    /** Optional short description shown below the category name. */
    private String description;

    /** URL of the category's cover image (external URL or uploaded path). */
    @Column(name = "image_url")
    private String imageUrl;
}
