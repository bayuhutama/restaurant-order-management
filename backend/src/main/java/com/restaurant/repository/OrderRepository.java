package com.restaurant.repository;

import com.restaurant.model.Order;
import com.restaurant.model.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Order entities.
 * Pessimistic locking is used where concurrent updates (e.g. payment confirmation)
 * could otherwise cause lost-update race conditions.
 *
 * @EntityGraph is applied to queries that drive mapToResponse() so that items,
 * payment, and user are loaded in the same query rather than triggering lazy
 * loads per order (N+1). The locking queries intentionally omit EntityGraph
 * to avoid interference with row-level lock scope.
 * Remaining lazy collections (e.g. items on list queries) are handled by the
 * global hibernate.default_batch_fetch_size=30 setting.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Eagerly loads items, payment, and user so mapToResponse() runs without
     * additional queries. Used for single-order lookups (order tracking page).
     */
    @EntityGraph(attributePaths = {"items", "payment", "user"})
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Acquires a pessimistic write lock on the order row before returning it.
     * Used in confirmPayment() to prevent double-payment if two requests arrive simultaneously.
     * No EntityGraph — JOIN FETCH and row-level locks don't compose cleanly.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberForUpdate(String orderNumber);

    /**
     * Acquires a pessimistic write lock on the order row by primary key.
     * Used in updateOrderStatus() to prevent two concurrent status-advance requests
     * from both reading the same current status and applying conflicting transitions.
     * No EntityGraph — same reasoning as findByOrderNumberForUpdate.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(Long id);

    /** Returns all orders for a registered customer, newest first. */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Returns all orders in the system, newest first — used by the admin panel. */
    List<Order> findAllByOrderByCreatedAtDesc();

    /** Returns orders whose status is in the given set, newest first. */
    List<Order> findByStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses);

    /** Returns orders whose status is NOT in the given set, newest first. */
    List<Order> findByStatusNotInOrderByCreatedAtDesc(List<OrderStatus> statuses);

    /**
     * Returns all orders that belong to a specific table session, with items and
     * payment eagerly loaded so mapToResponse() needs no further queries per order.
     */
    @EntityGraph(attributePaths = {"items", "payment", "user"})
    List<Order> findByTableSessionId(Long tableSessionId);
}
