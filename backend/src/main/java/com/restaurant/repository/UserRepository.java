package com.restaurant.repository;

import com.restaurant.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for User entities.
 * Authentication uses username; email uniqueness is also enforced at registration.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used by Spring Security's UserDetailsService to load a user for authentication. */
    Optional<User> findByUsername(String username);

    /** Checked during registration to prevent duplicate usernames. */
    boolean existsByUsername(String username);

    /** Available for email-based lookups if needed. */
    Optional<User> findByEmail(String email);

    /** Checked during registration to prevent duplicate email addresses. */
    boolean existsByEmail(String email);
}
