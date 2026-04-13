package com.restaurant.service;

import com.restaurant.dto.order.PaySessionRequest;
import com.restaurant.dto.order.TableSessionResponse;
import com.restaurant.model.Order;
import com.restaurant.model.Payment;
import com.restaurant.model.TableSession;
import com.restaurant.model.enums.PaymentMethod;
import com.restaurant.model.enums.PaymentStatus;
import com.restaurant.model.enums.TableSessionStatus;
import com.restaurant.repository.OrderRepository;
import com.restaurant.repository.PaymentRepository;
import com.restaurant.repository.TableSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableSessionServiceTest {

    @Mock TableSessionRepository tableSessionRepository;
    @Mock OrderRepository orderRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock OrderService orderService;
    @InjectMocks TableSessionService tableSessionService;

    private TableSession openSession;

    @BeforeEach
    void setUp() {
        openSession = TableSession.builder()
                .id(1L)
                .tableNumber("5")
                .status(TableSessionStatus.OPEN)
                .build();
        // Simulate @PrePersist
        openSession.setOpenedAt(LocalDateTime.now());
        openSession.setLastActivityAt(LocalDateTime.now());
    }

    // ── getOrCreateSession ────────────────────────────────────────────────────

    @Test
    void getOrCreateSession_createsNew_whenNoneExists() {
        when(tableSessionRepository.findByTableNumberAndStatusForUpdate("5", TableSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(tableSessionRepository.save(any(TableSession.class))).thenReturn(openSession);

        TableSession result = tableSessionService.getOrCreateSession("5");

        assertThat(result.getTableNumber()).isEqualTo("5");
        verify(tableSessionRepository).save(any(TableSession.class));
    }

    @Test
    void getOrCreateSession_returnsExisting_whenAlreadyOpen() {
        when(tableSessionRepository.findByTableNumberAndStatusForUpdate("5", TableSessionStatus.OPEN))
                .thenReturn(Optional.of(openSession));

        TableSession result = tableSessionService.getOrCreateSession("5");

        assertThat(result).isSameAs(openSession);
        verify(tableSessionRepository, never()).save(any());
    }

    // ── touchSession ──────────────────────────────────────────────────────────

    @Test
    void touchSession_updatesLastActivityAt() {
        LocalDateTime before = openSession.getLastActivityAt();
        when(tableSessionRepository.save(openSession)).thenReturn(openSession);

        tableSessionService.touchSession(openSession);

        assertThat(openSession.getLastActivityAt()).isAfterOrEqualTo(before);
        verify(tableSessionRepository).save(openSession);
    }

    // ── getOpenSession ────────────────────────────────────────────────────────

    @Test
    void getOpenSession_returnsEmpty_whenNoOpenSession() {
        when(tableSessionRepository.findByTableNumberAndStatus("99", TableSessionStatus.OPEN))
                .thenReturn(Optional.empty());

        Optional<TableSessionResponse> result = tableSessionService.getOpenSession("99");

        assertThat(result).isEmpty();
    }

    @Test
    void getOpenSession_returnsMappedResponse_whenSessionExists() {
        // orders is null → service falls back to repo query
        openSession.setOrders(null);
        when(tableSessionRepository.findByTableNumberAndStatus("5", TableSessionStatus.OPEN))
                .thenReturn(Optional.of(openSession));
        when(orderRepository.findByTableSessionId(1L)).thenReturn(List.of());

        Optional<TableSessionResponse> result = tableSessionService.getOpenSession("5");

        assertThat(result).isPresent();
        assertThat(result.get().tableNumber()).isEqualTo("5");
        assertThat(result.get().status()).isEqualTo(TableSessionStatus.OPEN);
        assertThat(result.get().totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── paySession ────────────────────────────────────────────────────────────

    @Test
    void paySession_marksSessionPaid_andPaymentsPaid() {
        Payment payment = Payment.builder()
                .id(1L)
                .status(PaymentStatus.PENDING)
                .method(PaymentMethod.CASH)
                .amount(BigDecimal.valueOf(50000))
                .build();

        Order order = Order.builder()
                .id(1L)
                .orderNumber("ORD-20260331-ABC123")
                .totalAmount(BigDecimal.valueOf(50000))
                .payment(payment)
                .build();

        when(tableSessionRepository.findByTableNumberAndStatusForUpdate("5", TableSessionStatus.OPEN))
                .thenReturn(Optional.of(openSession));
        when(orderRepository.findByTableSessionId(1L)).thenReturn(List.of(order));
        when(tableSessionRepository.save(any(TableSession.class))).thenReturn(openSession);

        PaySessionRequest request = new PaySessionRequest(PaymentMethod.CASH);
        tableSessionService.paySession("5", request);

        assertThat(openSession.getStatus()).isEqualTo(TableSessionStatus.PAID);
        assertThat(openSession.getClosedAt()).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        verify(paymentRepository).saveAll(anyList());
    }

    @Test
    void paySession_throws_whenNoOpenSession() {
        when(tableSessionRepository.findByTableNumberAndStatusForUpdate("5", TableSessionStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tableSessionService.paySession("5", new PaySessionRequest(PaymentMethod.CASH)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No active session for table: 5");
    }

    @Test
    void paySession_skipsAlreadyPaidPayments() {
        Payment alreadyPaid = Payment.builder()
                .id(2L)
                .status(PaymentStatus.PAID)
                .method(PaymentMethod.CARD)
                .amount(BigDecimal.valueOf(75000))
                .build();

        Order order = Order.builder()
                .id(2L)
                .orderNumber("ORD-20260331-XYZ789")
                .totalAmount(BigDecimal.valueOf(75000))
                .payment(alreadyPaid)
                .build();

        when(tableSessionRepository.findByTableNumberAndStatusForUpdate("5", TableSessionStatus.OPEN))
                .thenReturn(Optional.of(openSession));
        when(orderRepository.findByTableSessionId(1L)).thenReturn(List.of(order));
        when(tableSessionRepository.save(any())).thenReturn(openSession);

        tableSessionService.paySession("5", new PaySessionRequest(PaymentMethod.CASH));

        // Already-PAID payment should not trigger saveAll (empty list is skipped)
        verify(paymentRepository, never()).saveAll(any());
    }

    // ── expireInactiveSessions ────────────────────────────────────────────────

    @Test
    void expireInactiveSessions_callsBulkExpire_andLogsWhenRowsUpdated() {
        when(tableSessionRepository.bulkExpireInactiveSessions(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2);

        tableSessionService.expireInactiveSessions();

        verify(tableSessionRepository).bulkExpireInactiveSessions(
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void expireInactiveSessions_doesNothing_whenNoStaleSessions() {
        when(tableSessionRepository.bulkExpireInactiveSessions(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0);

        tableSessionService.expireInactiveSessions();

        verify(tableSessionRepository).bulkExpireInactiveSessions(
                any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
