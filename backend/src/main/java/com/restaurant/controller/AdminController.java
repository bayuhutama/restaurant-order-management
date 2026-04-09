package com.restaurant.controller;

import com.restaurant.dto.menu.CategoryRequest;
import com.restaurant.dto.menu.CategoryResponse;
import com.restaurant.dto.menu.MenuItemRequest;
import com.restaurant.dto.menu.MenuItemResponse;
import com.restaurant.dto.order.OrderResponse;
import com.restaurant.service.MenuService;
import com.restaurant.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only endpoints for managing menu content and orders.
 * All routes require the ADMIN role (enforced by @PreAuthorize at class level).
 * Staff use StaffController for order status updates; admins can also cancel orders here.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final MenuService menuService;
    private final OrderService orderService;

    // ── Categories ───────────────────────────────────────────────────────────

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(menuService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(menuService.updateCategory(id, request));
    }

    /** Deletes a category. Note: menu items linked to it will lose their category. */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        menuService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ── Menu Items ───────────────────────────────────────────────────────────

    @PostMapping("/menu")
    public ResponseEntity<MenuItemResponse> createMenuItem(@Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuService.createMenuItem(request));
    }

    @PutMapping("/menu/{id}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable Long id, @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuService.updateMenuItem(id, request));
    }

    @DeleteMapping("/menu/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        menuService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }

    /** Flips the item's available flag between true and false. */
    @PatchMapping("/menu/{id}/availability")
    public ResponseEntity<MenuItemResponse> toggleAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.toggleAvailability(id));
    }

    // ── Orders ───────────────────────────────────────────────────────────────

    /** Returns all orders including AWAITING_PAYMENT — visible only to admins. */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /** Allows admins to set any status, including CANCELLED. */
    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody com.restaurant.dto.order.UpdateStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.status()));
    }
}
