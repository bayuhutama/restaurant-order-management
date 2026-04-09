package com.restaurant.repository;

import com.restaurant.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for MenuItem entities.
 * Custom finders support the filtering logic in MenuService.getAllMenuItems().
 */
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /** Returns all items (available or not) belonging to the given category. */
    List<MenuItem> findByCategoryId(Long categoryId);

    /** Returns only items that are currently orderable (available = true). */
    List<MenuItem> findByAvailableTrue();

    /** Returns available items filtered by category — used when both filters are active. */
    List<MenuItem> findByCategoryIdAndAvailableTrue(Long categoryId);
}
