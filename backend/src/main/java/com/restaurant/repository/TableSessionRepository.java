package com.restaurant.repository;

import com.restaurant.model.TableSession;
import com.restaurant.model.enums.TableSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for TableSession entities.
 * Pessimistic locking is used when creating or closing sessions to prevent
 * duplicate sessions from being created by concurrent order requests at the same table.
 */
@Repository
public interface TableSessionRepository extends JpaRepository<TableSession, Long> {

    /**
     * Acquires a pessimistic write lock before returning the active session for a table.
     * Used in getOrCreateSession() so only one thread can open a new session at a time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TableSession s WHERE s.tableNumber = :tableNumber AND s.status = :status")
    Optional<TableSession> findByTableNumberAndStatusForUpdate(String tableNumber, TableSessionStatus status);

    /** Read-only lookup of the active session for a given table. */
    Optional<TableSession> findByTableNumberAndStatus(String tableNumber, TableSessionStatus status);

    /**
     * Finds OPEN sessions whose last activity timestamp is older than the cutoff.
     * Used by the @Scheduled expiry job to auto-expire idle tables.
     */
    List<TableSession> findByStatusAndLastActivityAtBefore(TableSessionStatus status, LocalDateTime cutoff);

    /** Returns all sessions in the given status ordered by when they were opened, newest first. */
    List<TableSession> findByStatusOrderByOpenedAtDesc(TableSessionStatus status);
}
