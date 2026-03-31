package com.restaurant.controller;

import com.restaurant.dto.order.OrderResponse;
import com.restaurant.dto.order.UpdateStatusRequest;
import com.restaurant.model.Order;
import com.restaurant.model.enums.OrderStatus;
import com.restaurant.repository.OrderRepository;
import com.restaurant.service.OrderService;
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
