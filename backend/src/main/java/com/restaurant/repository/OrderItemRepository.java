package com.restaurant.repository;

import com.restaurant.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for OrderItem entities.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Returns all line items belonging to the given order. */
    List<OrderItem> findByOrderId(Long orderId);
}
