package com.restaurant.service;

import com.restaurant.dto.order.OrderItemRequest;
import com.restaurant.dto.order.OrderRequest;
import com.restaurant.dto.order.OrderResponse;
import com.restaurant.model.*;
import com.restaurant.model.enums.OrderStatus;
import com.restaurant.model.enums.PaymentMethod;
import com.restaurant.model.enums.PaymentStatus;
import com.restaurant.model.enums.TableSessionStatus;
import com.restaurant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock MenuItemRepository menuItemRepository;
    @Mock UserRepository userRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock TableSessionService tableSessionService;

    @InjectMocks OrderService orderService;

    private MenuItem availableItem;
    private MenuItem unavailableItem;
    private TableSession openSession;
    private Order pendingOrder;
    private Payment cashPayment;

    @BeforeEach
    void setUp() {
        // Inject tableSessionService via the setter (bypasses @Lazy)
        ReflectionTestUtils.setField(orderService, "tableSessionService", tableSessionService);

        availableItem = MenuItem.builder()
                .id(1L)
                .name("Nasi Goreng")
                .price(BigDecimal.valueOf(35000))
                .available(true)
                .build();

        unavailableItem = MenuItem.builder()
                .id(2L)
                .name("Sold Out Item")
                .price(BigDecimal.valueOf(25000))
                .available(false)
                .build();

        openSession = TableSession.builder()
                .id(1L)
                .tableNumber("3")
                .status(TableSessionStatus.OPEN)
                .build();
        openSession.setOpenedAt(LocalDateTime.now());
        openSession.setLastActivityAt(LocalDateTime.now());

        cashPayment = Payment.builder()
                .id(1L)
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PENDING)
                .amount(BigDecimal.valueOf(35000))
                .build();

        pendingOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-20260331-ABC123")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(35000))
                .tableNumber("3")
                .tableSession(openSession)
                .payment(cashPayment)
                .build();
        pendingOrder.setItems(List.of());
        pendingOrder.setCreatedAt(LocalDateTime.now());
        pendingOrder.setUpdatedAt(LocalDateTime.now());
    }

    // ── placeOrder ────────────────────────────────────────────────────────────

    @Test
    void placeOrder_createsOrderInPendingState() {
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(1L, 2, "")),
                "no onions",
                "3",
                "Guest A", "08111111111", null,
                PaymentMethod.CASH
        );

        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(availableItem));
        when(tableSessionService.getOrCreateSession("3")).thenReturn(openSession);
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenReturn(cashPayment);

        OrderResponse response = orderService.placeOrder(request);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.tableNumber()).isEqualTo("3");
        verify(tableSessionService).getOrCreateSession("3");
        verify(tableSessionService).touchSession(openSession);
        verify(messagingTemplate).convertAndSend(eq("/topic/orders"), any(OrderResponse.class));
    }

    @Test
    void placeOrder_throws_whenMenuItemNotFound() {
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(999L, 1, "")),
                null, "3", null, null, null, PaymentMethod.CASH
        );

        when(menuItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Menu item not found: 999");
    }

    @Test
    void placeOrder_throws_whenItemUnavailable() {
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(2L, 1, "")),
                null, "3", null, null, null, PaymentMethod.CASH
        );

        when(menuItemRepository.findById(2L)).thenReturn(Optional.of(unavailableItem));

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void placeOrder_calculatesCorrectTotal() {
        // 2 × 35000 = 70000
        OrderItemRequest itemReq = new OrderItemRequest(1L, 2, "");
        OrderRequest request = new OrderRequest(
                List.of(itemReq), null, "3", null, null, null, PaymentMethod.CASH
        );

        Order savedOrder = Order.builder()
                .id(2L)
                .orderNumber("ORD-20260331-DEF456")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(70000))
                .tableNumber("3")
                .tableSession(openSession)
                .payment(cashPayment)
                .build();
        savedOrder.setItems(List.of());
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setUpdatedAt(LocalDateTime.now());

        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(availableItem));
        when(tableSessionService.getOrCreateSession("3")).thenReturn(openSession);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            // Verify the total was calculated as 2 × 35000
            assertThat(o.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(70000));
            return savedOrder;
        });
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());
        when(paymentRepository.save(any())).thenReturn(cashPayment);

        orderService.placeOrder(request);
    }

    // ── updateOrderStatus ─────────────────────────────────────────────────────

    @Test
    void updateOrderStatus_changesStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(messagingTemplate).convertAndSend(eq("/topic/orders"), any(OrderResponse.class));
        verify(messagingTemplate).convertAndSend(
                eq("/topic/orders/" + pendingOrder.getOrderNumber()), any(OrderResponse.class));
    }

    @Test
    void updateOrderStatus_delivered_marksCashPaymentAsPaid() {
        // Must be in READY state to transition to DELIVERED
        pendingOrder.setStatus(OrderStatus.READY);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);

        orderService.updateOrderStatus(1L, OrderStatus.DELIVERED);

        assertThat(cashPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(cashPayment.getPaidAt()).isNotNull();
        verify(paymentRepository).save(cashPayment);
    }

    @Test
    void updateOrderStatus_delivered_doesNotOverwriteCardPayment() {
        Payment cardPayment = Payment.builder()
                .id(2L)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.PAID) // already paid
                .amount(BigDecimal.valueOf(35000))
                .build();
        pendingOrder.setStatus(OrderStatus.READY);
        pendingOrder.setPayment(cardPayment);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);

        orderService.updateOrderStatus(1L, OrderStatus.DELIVERED);

        // Card payment already PAID — paymentRepository.save should NOT be called
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_invalidTransition_throws() {
        // DELIVERED is a terminal state — no further transitions allowed
        pendingOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot transition order from DELIVERED to PENDING");
    }

    @Test
    void updateOrderStatus_throws_whenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(999L, OrderStatus.CONFIRMED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    // ── getOrderByNumber ──────────────────────────────────────────────────────

    @Test
    void getOrderByNumber_returnsOrder() {
        when(orderRepository.findByOrderNumber("ORD-20260331-ABC123"))
                .thenReturn(Optional.of(pendingOrder));

        OrderResponse response = orderService.getOrderByNumber("ORD-20260331-ABC123");

        assertThat(response.orderNumber()).isEqualTo("ORD-20260331-ABC123");
    }

    @Test
    void getOrderByNumber_throws_whenNotFound() {
        when(orderRepository.findByOrderNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByNumber("INVALID"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found: INVALID");
    }
}
