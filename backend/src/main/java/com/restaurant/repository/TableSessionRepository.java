package com.restaurant.repository;

import com.restaurant.model.TableSession;
import com.restaurant.model.enums.TableSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Returns all sessions in the given status ordered by when they were opened, newest first. */
    List<TableSession> findByStatusOrderByOpenedAtDesc(TableSessionStatus status);

    /**
     * Atomically marks all OPEN sessions that have been idle past the cutoff as EXPIRED.
     *
     * This bulk UPDATE replaces the previous read-then-save loop, which had a race window
     * where {@code paySession()} could commit status=PAID between the scheduler's SELECT
     * and its save(), causing the scheduler to silently overwrite PAID → EXPIRED.
     *
     * The WHERE clause only matches rows whose status is still OPEN at the instant the
     * UPDATE executes, so a concurrently committed PAID/EXPIRED session is safely excluded.
     *
     * @param cutoff sessions idle since before this timestamp are expired
     * @param now    timestamp written to closed_at for all newly expired sessions
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE TableSession s SET s.status = com.restaurant.model.enums.TableSessionStatus.EXPIRED, " +
           "s.closedAt = :now " +
           "WHERE s.status = com.restaurant.model.enums.TableSessionStatus.OPEN " +
           "AND s.lastActivityAt < :cutoff")
    int bulkExpireInactiveSessions(@Param("cutoff") LocalDateTime cutoff, @Param("now") LocalDateTime now);
}
