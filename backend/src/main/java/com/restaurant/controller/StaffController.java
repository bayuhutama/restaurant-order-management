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

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final TableSessionService tableSessionService;

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(orderService.getActiveOrders());
        }
        return ResponseEntity.ok(orderService.getStaffOrders());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return ResponseEntity.ok(orderService.mapToResponse(order));
    }

    @GetMapping("/tables")
    public ResponseEntity<List<TableSessionResponse>> getOpenSessions() {
        return ResponseEntity.ok(tableSessionService.getOpenSessions());
    }

    @PostMapping("/tables/{tableNumber}/close")
    public ResponseEntity<Void> closeSession(@PathVariable String tableNumber) {
        tableSessionService.closeSession(tableNumber);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        if (request.status() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Orders cannot be cancelled once payment is confirmed");
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.status()));
    }
}
