package com.restaurant.controller;

import com.restaurant.dto.order.OrderRequest;
import com.restaurant.dto.order.OrderResponse;
import com.restaurant.dto.order.PaymentConfirmRequest;
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
     * The response includes a one-time {@code payment.paymentToken} that the
     * customer must store and supply when calling the pay endpoint.
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
     * Requires the {@code paymentToken} that was issued in the placeOrder response.
     * This prevents an unauthenticated third party who knows only the order number
     * (e.g. by watching the public WebSocket) from marking a stranger's order as PAID.
     * After this, staff can advance the order from PENDING → CONFIRMED.
     */
    @PostMapping("/{orderNumber}/pay")
    public ResponseEntity<OrderResponse> confirmPayment(
            @PathVariable String orderNumber,
            @Valid @RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.ok(orderService.confirmPayment(orderNumber, request.paymentToken()));
    }
}
