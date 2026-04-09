package com.restaurant.controller;

import com.restaurant.dto.order.OrderRequest;
import com.restaurant.dto.order.OrderResponse;
import com.restaurant.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Customer-facing order endpoints — all public (no authentication required).
 *
 * POST /api/orders          — place an order (guest or authenticated user)
 * GET  /api/orders/track/:n — poll the current status of an order
 * POST /api/orders/:n/pay   — customer confirms payment on the payment page
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Places a new order and creates a pending payment record.
     * Assigns the order to an existing or new table session.
     * Notifies staff via WebSocket immediately after saving.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(request));
    }

    /** Returns the current state of an order — used for live status tracking. */
    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<OrderResponse> trackOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }

    /**
     * Marks the order's payment as PAID.
     * After this, staff can advance the order from PENDING → CONFIRMED.
     */
    @PostMapping("/{orderNumber}/pay")
    public ResponseEntity<OrderResponse> confirmPayment(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.confirmPayment(orderNumber));
    }
}
