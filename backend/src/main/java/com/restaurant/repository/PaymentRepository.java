package com.restaurant.repository;

import com.restaurant.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for Payment entities.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Looks up the payment record for a specific order by its ID. */
    Optional<Payment> findByOrderId(Long orderId);
}
