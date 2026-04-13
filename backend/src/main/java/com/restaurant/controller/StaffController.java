package com.restaurant.controller;

import com.restaurant.dto.order.OrderResponse;
import com.restaurant.dto.order.TableSessionResponse;
import com.restaurant.dto.order.UpdateStatusRequest;
import com.restaurant.model.Order;
import com.restaurant.model.enums.OrderStatus;
import com.restaurant.repository.OrderRepository;
import com.restaurant.service.OrderService;
import com.restaurant.service.TableSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Staff (and admin) endpoints for the kitchen/service dashboard.
 * Requires STAFF or ADMIN role on all routes.
 *
 * Staff can view and advance orders but CANNOT cancel them —
 * cancellation is blocked here and only allowed via AdminController.
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final TableSessionService tableSessionService;

    /**
     * Returns orders for the dashboard.
     * @param activeOnly when true, only returns in-progress orders (PENDING/CONFIRMED/PREPARING/READY)
     */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(orderService.getActiveOrders());
        }
        return ResponseEntity.ok(orderService.getStaffOrders());
    }

    /** Returns a single order by database ID. */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return ResponseEntity.ok(orderService.mapToResponse(order));
    }

    /** Returns all currently OPEN table sessions for the active tables widget. */
    @GetMapping("/tables")
    public ResponseEntity<List<TableSessionResponse>> getOpenSessions() {
        return ResponseEntity.ok(tableSessionService.getOpenSessions());
    }

    /**
     * Staff manually ends a table session (e.g. after customers leave).
     * Marks the session EXPIRED, freeing the table for the next group.
     */
    @PostMapping("/tables/{tableNumber}/close")
    public ResponseEntity<Void> closeSession(@PathVariable String tableNumber) {
        tableSessionService.closeSession(tableNumber);
        return ResponseEntity.ok().build();
    }

    /**
     * Advances an order to the next status.
     * Cancellation is explicitly blocked — only admins may cancel orders.
     * Moving PENDING → CONFIRMED also requires payment.status == PAID,
     * enforced inside OrderService.updateOrderStatus().
     */
    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        if (request.status() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Orders cannot be cancelled by staff — contact an admin");
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.status()));
    }
}
