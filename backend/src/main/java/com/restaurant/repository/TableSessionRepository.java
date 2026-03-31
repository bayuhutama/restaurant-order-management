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

@Repository
public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TableSession s WHERE s.tableNumber = :tableNumber AND s.status = :status")
    Optional<TableSession> findByTableNumberAndStatusForUpdate(String tableNumber, TableSessionStatus status);

    Optional<TableSession> findByTableNumberAndStatus(String tableNumber, TableSessionStatus status);
    List<TableSession> findByStatusAndLastActivityAtBefore(TableSessionStatus status, LocalDateTime cutoff);
    List<TableSession> findByStatusOrderByOpenedAtDesc(TableSessionStatus status);
}
