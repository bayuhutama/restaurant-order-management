package com.restaurant.repository;

import com.restaurant.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access layer for Category entities.
 * Inherits standard CRUD operations from JpaRepository.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
