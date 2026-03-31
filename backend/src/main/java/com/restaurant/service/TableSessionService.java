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

@Slf4j
@Service
@RequiredArgsConstructor
public class TableSessionService {

    private final TableSessionRepository tableSessionRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    @Value("${table.session.timeout-minutes:60}")
    private int sessionTimeoutMinutes;

    @Transactional
    public TableSession getOrCreateSession(String tableNumber) {
        return tableSessionRepository
                .findByTableNumberAndStatusForUpdate(tableNumber, TableSessionStatus.OPEN)
                .orElseGet(() -> tableSessionRepository.save(
                        TableSession.builder()
                                .tableNumber(tableNumber)
                                .status(TableSessionStatus.OPEN)
                                .build()
                ));
    }

    @Transactional
    public void touchSession(TableSession session) {
        session.setLastActivityAt(LocalDateTime.now());
        tableSessionRepository.save(session);
    }

    public Optional<TableSessionResponse> getOpenSession(String tableNumber) {
        return tableSessionRepository
                .findByTableNumberAndStatus(tableNumber, TableSessionStatus.OPEN)
                .map(this::mapToResponse);
    }

    @Transactional
    public TableSessionResponse paySession(String tableNumber, PaySessionRequest request) {
        TableSession session = tableSessionRepository
                .findByTableNumberAndStatusForUpdate(tableNumber, TableSessionStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("No active session for table: " + tableNumber));

        List<Order> orders = orderRepository.findByTableSessionId(session.getId());
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
        saved.setOrders(orders);
        return mapToResponse(saved);
    }

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

    private TableSessionResponse mapToResponse(TableSession session) {
        List<Order> orders = session.getOrders() != null
                ? session.getOrders()
                : orderRepository.findByTableSessionId(session.getId());

        List<OrderResponse> orderResponses = orders.stream()
                .map(orderService::mapToResponse)
                .toList();

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
