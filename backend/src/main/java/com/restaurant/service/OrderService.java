package com.restaurant.service;

import com.restaurant.dto.order.*;
import com.restaurant.model.*;
import com.restaurant.model.enums.OrderStatus;
import com.restaurant.model.enums.PaymentMethod;
import com.restaurant.model.enums.PaymentStatus;
import com.restaurant.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Core business logic for order placement, payment confirmation, and status management.
 *
 * WebSocket broadcasts are sent to three topics after every mutating operation:
 *   /topic/orders               — staff dashboard (receives all updates)
 *   /topic/orders/{number}      — individual customer order tracking page
 *   /topic/table/{tableNumber}  — all customers at the same table (enables real-time
 *                                  synchronization of the shared "My Orders" view)
 *
 * Circular dependency note: OrderService ↔ TableSessionService.
 * TableSessionService is injected via @Lazy setter injection to break the cycle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Injected lazily to break the circular dependency with TableSessionService
    private TableSessionService tableSessionService;

    @Autowired
    public void setTableSessionService(@Lazy TableSessionService tableSessionService) {
        this.tableSessionService = tableSessionService;
    }

    /**
     * Creates the order in PENDING state and assigns it to a table session.
     * Staff are notified via WebSocket immediately.
     */
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // Validate all items exist and are available, then build line items
        for (OrderItemRequest itemReq : request.items()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.menuItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemReq.menuItemId()));

            if (!menuItem.isAvailable()) {
                throw new RuntimeException("Menu item is not available: " + menuItem.getName());
            }

            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .quantity(itemReq.quantity())
                    .unitPrice(menuItem.getPrice())  // snapshot price at order time
                    .notes(itemReq.notes())
                    .build();

            items.add(orderItem);
            total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
        }

        // Associate with table session (creates one if none is currently OPEN)
        TableSession session = tableSessionService.getOrCreateSession(request.tableNumber());

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .notes(request.notes())
                .tableNumber(request.tableNumber())
                .tableSession(session)
                .build();

        order.setGuestName(request.guestName());
        order.setGuestPhone(request.guestPhone());
        order.setGuestEmail(request.guestEmail());

        Order savedOrder = orderRepository.save(order);

        // Link items to the saved order and persist them
        for (OrderItem item : items) {
            item.setOrder(savedOrder);
        }
        List<OrderItem> savedItems = orderItemRepository.saveAll(items);
        savedOrder.setItems(savedItems);

        // Create the payment record in PENDING state
        Payment payment = Payment.builder()
                .order(savedOrder)
                .method(request.paymentMethod())
                .amount(total)
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        savedOrder.setPayment(savedPayment);

        // Update session's lastActivityAt so the inactivity timer resets
        tableSessionService.touchSession(session);

        log.info("Order placed: orderNumber={}, table={}, items={}, total={}",
                savedOrder.getOrderNumber(), request.tableNumber(), items.size(), total);

        OrderResponse response = mapToResponse(savedOrder);
        // Notify staff immediately — no pre-payment required to show on dashboard
        broadcast(response);

        return response;
    }

    /**
     * Customer confirms payment on the payment page.
     * Marks the payment as PAID and notifies staff via WebSocket.
     * Uses pessimistic locking to prevent double-payment race conditions.
     */
    @Transactional
    public OrderResponse confirmPayment(String orderNumber) {
        Order order = orderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new RuntimeException("Payment record not found");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Order has already been paid");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Order saved = orderRepository.save(order);
        saved.setPayment(payment);

        log.info("Payment confirmed: orderNumber={}, txnId={}", saved.getOrderNumber(), payment.getTransactionId());

        OrderResponse response = mapToResponse(saved);
        broadcast(response);

        return response;
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
        return mapToResponse(order);
    }

    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).toList();
    }

    /** All orders including AWAITING_PAYMENT — for admin only. */
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::mapToResponse).toList();
    }

    /** Orders visible to staff — excludes AWAITING_PAYMENT (legacy status). */
    public List<OrderResponse> getStaffOrders() {
        return orderRepository.findByStatusNotInOrderByCreatedAtDesc(
                        List.of(OrderStatus.AWAITING_PAYMENT))
                .stream().map(this::mapToResponse).toList();
    }

    /** Active orders for staff dashboard (in-progress only — not delivered or cancelled). */
    public List<OrderResponse> getActiveOrders() {
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING, OrderStatus.CONFIRMED,
                OrderStatus.PREPARING, OrderStatus.READY
        );
        return orderRepository.findByStatusInOrderByCreatedAtDesc(activeStatuses)
                .stream().map(this::mapToResponse).toList();
    }

    /**
     * Allowed status transitions for each order state.
     * Any transition not listed here will throw an exception.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING,    Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED,  Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING,  Set.of(OrderStatus.READY),
            OrderStatus.READY,      Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED,  Set.of(),
            OrderStatus.CANCELLED,  Set.of(),
            OrderStatus.AWAITING_PAYMENT, Set.of(OrderStatus.PENDING, OrderStatus.CANCELLED)
    );

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        // Pessimistic write lock prevents two concurrent status-advance requests from
        // both reading the same current status and racing through the transition check.
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Validate the requested transition is legal
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new RuntimeException(
                    "Cannot transition order from " + order.getStatus() + " to " + newStatus);
        }

        // Require payment before staff can confirm an order
        if (order.getStatus() == OrderStatus.PENDING && newStatus == OrderStatus.CONFIRMED) {
            Payment payment = order.getPayment();
            if (payment == null || payment.getStatus() != PaymentStatus.PAID) {
                throw new RuntimeException("Cannot confirm order: payment has not been received");
            }
        }

        log.info("Order status updated: id={}, {} -> {}", id, order.getStatus(), newStatus);
        order.setStatus(newStatus);

        // Auto-mark cash payment as paid when the order is delivered
        if (newStatus == OrderStatus.DELIVERED && order.getPayment() != null
                && order.getPayment().getMethod() == PaymentMethod.CASH
                && order.getPayment().getStatus() == PaymentStatus.PENDING) {
            order.getPayment().setStatus(PaymentStatus.PAID);
            order.getPayment().setPaidAt(LocalDateTime.now());
            paymentRepository.save(order.getPayment());
        }

        Order saved = orderRepository.save(order);
        OrderResponse response = mapToResponse(saved);
        broadcast(response);

        return response;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Broadcasts an order update to all three relevant WebSocket topics:
     *   - /topic/orders              — staff dashboard
     *   - /topic/orders/{number}     — the individual customer's tracking page
     *   - /topic/table/{tableNumber} — all customers at the same table (shared view sync)
     */
    private void broadcast(OrderResponse response) {
        messagingTemplate.convertAndSend("/topic/orders", response);
        messagingTemplate.convertAndSend("/topic/orders/" + response.orderNumber(), response);
        messagingTemplate.convertAndSend("/topic/table/" + response.tableNumber(), response);
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    /**
     * Maps an Order entity to its response DTO.
     * Resolves customer identity: prefers User fields, falls back to guest fields.
     * Public so StaffController can call it directly for single-order lookups.
     */
    public OrderResponse mapToResponse(Order order) {
        List<OrderItem> items = order.getItems() != null ? order.getItems() : List.of();

        List<OrderItemResponse> itemResponses = items.stream().map(item ->
                new OrderItemResponse(
                        item.getId(),
                        item.getMenuItem().getId(),
                        item.getMenuItem().getName(),
                        item.getMenuItem().getImageUrl(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                        item.getNotes()
                )
        ).toList();

        PaymentResponse paymentResponse = null;
        if (order.getPayment() != null) {
            Payment p = order.getPayment();
            paymentResponse = new PaymentResponse(p.getId(), p.getMethod(), p.getStatus(),
                    p.getAmount(), p.getTransactionId(), p.getPaidAt());
        }

        // Prefer registered user data; fall back to guest fields for unauthenticated orders
        String customerName  = order.getUser() != null ? order.getUser().getName()  : order.getGuestName();
        String customerPhone = order.getUser() != null ? order.getUser().getPhone() : order.getGuestPhone();
        String customerEmail = order.getUser() != null ? order.getUser().getEmail() : order.getGuestEmail();

        return new OrderResponse(
                order.getId(), order.getOrderNumber(),
                customerName, customerPhone, customerEmail,
                order.getStatus(), order.getTotalAmount(),
                order.getNotes(), order.getTableNumber(),
                itemResponses, paymentResponse,
                order.getCreatedAt(), order.getUpdatedAt()
        );
    }

    /**
     * Generates a human-readable order number in the format ORD-YYYYMMDD-XXXXXX.
     * The random suffix reduces collisions under concurrent load.
     */
    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + date + "-" + random;
    }
}
