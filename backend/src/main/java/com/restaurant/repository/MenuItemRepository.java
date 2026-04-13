package com.restaurant.repository;

import com.restaurant.model.MenuItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for MenuItem entities.
 * Custom finders support the filtering logic in MenuService.getAllMenuItems().
 *
 * All list-returning methods use @EntityGraph to JOIN FETCH the category association,
 * eliminating N+1 queries when the mapper accesses m.getCategory() for each item.
 * (category is ManyToOne — a single JOIN, no Cartesian-product risk.)
 */
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /**
     * Overrides JpaRepository.findAll() to eagerly load the category for each item,
     * preventing an N+1 query when all items are listed without a category filter.
     */
    @Override
    @EntityGraph(attributePaths = {"category"})
    List<MenuItem> findAll();

    /** Returns all items (available or not) belonging to the given category. */
    @EntityGraph(attributePaths = {"category"})
    List<MenuItem> findByCategoryId(Long categoryId);

    /** Returns only items that are currently orderable (available = true). */
    @EntityGraph(attributePaths = {"category"})
    List<MenuItem> findByAvailableTrue();

    /** Returns available items filtered by category — used when both filters are active. */
    @EntityGraph(attributePaths = {"category"})
    List<MenuItem> findByCategoryIdAndAvailableTrue(Long categoryId);
}
