package com.restaurant.service;

import com.restaurant.dto.order.OrderResponse;
import com.restaurant.dto.order.PaySessionRequest;
import com.restaurant.dto.order.TableSessionResponse;
import com.restaurant.model.Order;
import com.restaurant.model.Payment;
import com.restaurant.model.TableSession;
import com.restaurant.model.enums.PaymentStatus;
import com.restaurant.model.enums.TableSessionStatus;
import com.restaurant.repository.OrderRepository;
import com.restaurant.repository.PaymentRepository;
import com.restaurant.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Manages table sessions — groups of orders placed at the same table during one visit.
 *
 * Key responsibilities:
 * - getOrCreateSession: find or open a session when the first order arrives
 * - paySession: settle the whole table's bill in one action
 * - closeSession: staff manually end a session (e.g. after customers leave)
 * - expireInactiveSessions: scheduled job that auto-expires idle tables
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableSessionService {

    private final TableSessionRepository tableSessionRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    /** How long (minutes) a session can be idle before the scheduler expires it. Default: 60. */
    @Value("${table.session.timeout-minutes:60}")
    private int sessionTimeoutMinutes;

    /**
     * Returns the existing OPEN session for the table, or creates a new one.
     * Uses pessimistic locking to prevent duplicate sessions when multiple
     * order requests arrive for the same table simultaneously.
     */
    @Transactional
    public TableSession getOrCreateSession(String tableNumber) {
        return tableSessionRepository
                .findByTableNumberAndStatusForUpdate(tableNumber, TableSessionStatus.OPEN)
                .orElseGet(() -> {
                    TableSession session = tableSessionRepository.save(
                            TableSession.builder()
                                    .tableNumber(tableNumber)
                                    .status(TableSessionStatus.OPEN)
                                    .build()
                    );
                    log.info("Session opened: table={}, sessionId={}", tableNumber, session.getId());
                    return session;
                });
    }

    /**
     * Updates lastActivityAt on the session after an order is placed.
     * This resets the inactivity timer used by the expiry scheduler.
     */
    @Transactional
    public void touchSession(TableSession session) {
        session.setLastActivityAt(LocalDateTime.now());
        tableSessionRepository.save(session);
    }

    /**
     * Returns the active session for a table as a response DTO, or empty if none.
     * Used by the customer-facing /api/table-sessions/{tableNumber} endpoint.
     */
    public Optional<TableSessionResponse> getOpenSession(String tableNumber) {
        return tableSessionRepository
                .findByTableNumberAndStatus(tableNumber, TableSessionStatus.OPEN)
                .map(this::mapToResponse);
    }

    /** Returns all currently open sessions ordered by when they were opened (newest first). */
    public List<TableSessionResponse> getOpenSessions() {
        return tableSessionRepository
                .findByStatusOrderByOpenedAtDesc(TableSessionStatus.OPEN)
                .stream().map(this::mapToResponse).toList();
    }

    /**
     * Staff manually closes a session (e.g. after customers leave without paying via app).
     * Marks the session EXPIRED, freeing the table for the next group.
     */
    @Transactional
    public void closeSession(String tableNumber) {
        TableSession session = tableSessionRepository
                .findByTableNumberAndStatusForUpdate(tableNumber, TableSessionStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("No active session for table: " + tableNumber));
        session.setStatus(TableSessionStatus.EXPIRED);
        session.setClosedAt(LocalDateTime.now());
        tableSessionRepository.save(session);
        log.info("Session closed by staff: table={}, sessionId={}", tableNumber, session.getId());
    }

    /**
     * Settles the entire table bill in one action.
     * Marks all pending payment records as PAID, records the payment method,
     * and transitions the session to PAID.
     */
    @Transactional
    public TableSessionResponse paySession(String tableNumber, PaySessionRequest request) {
        TableSession session = tableSessionRepository
                .findByTableNumberAndStatusForUpdate(tableNumber, TableSessionStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("No active session for table: " + tableNumber));

        List<Order> orders = orderRepository.findByTableSessionId(session.getId());

        // Mark all unpaid order payments as PAID with the chosen method
        for (Order order : orders) {
            Payment payment = order.getPayment();
            if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setMethod(request.paymentMethod());
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        }

        session.setStatus(TableSessionStatus.PAID);
        session.setClosedAt(LocalDateTime.now());
        session.setPaymentMethod(request.paymentMethod());
        TableSession saved = tableSessionRepository.save(session);
        saved.setOrders(orders);  // attach for the response mapper (avoids a second DB query)
        log.info("Session paid: table={}, sessionId={}, method={}", tableNumber, session.getId(), request.paymentMethod());
        return mapToResponse(saved);
    }

    /**
     * Runs every 60 seconds.
     * Finds all OPEN sessions whose lastActivityAt is older than the configured timeout
     * and marks them EXPIRED to free up idle tables automatically.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireInactiveSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
        List<TableSession> expired = tableSessionRepository
                .findByStatusAndLastActivityAtBefore(TableSessionStatus.OPEN, cutoff);
        for (TableSession session : expired) {
            session.setStatus(TableSessionStatus.EXPIRED);
            session.setClosedAt(LocalDateTime.now());
            tableSessionRepository.save(session);
            log.info("Expired session for table {} (inactive since {})", session.getTableNumber(), session.getLastActivityAt());
        }
    }

    /** Builds a TableSessionResponse from a session entity, fetching orders if not already loaded. */
    private TableSessionResponse mapToResponse(TableSession session) {
        List<Order> orders = session.getOrders() != null
                ? session.getOrders()
                : orderRepository.findByTableSessionId(session.getId());

        List<OrderResponse> orderResponses = orders.stream()
                .map(orderService::mapToResponse)
                .toList();

        // Total is the sum of all individual order amounts for this table visit
        BigDecimal total = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TableSessionResponse(
                session.getId(),
                session.getTableNumber(),
                session.getStatus(),
                total,
                orders.size(),
                session.getOpenedAt(),
                session.getLastActivityAt(),
                session.getPaymentMethod(),
                orderResponses
        );
    }
}
