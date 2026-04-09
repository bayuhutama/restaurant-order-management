package com.restaurant.controller;

import com.restaurant.dto.menu.CategoryResponse;
import com.restaurant.dto.menu.MenuItemResponse;
import com.restaurant.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public read-only endpoints for browsing the menu.
 * No authentication is required — customers and guests can both access these.
 * Write operations (create/update/delete) are in AdminController.
 */
@RestController
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // ── Categories (public) ──────────────────────────────────────────────────

    @GetMapping("/api/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(menuService.getAllCategories());
    }

    @GetMapping("/api/categories/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getCategoryById(id));
    }

    // ── Menu Items (public) ──────────────────────────────────────────────────

    /**
     * Returns menu items with optional filters.
     * @param categoryId  filter to a specific category (null = all categories)
     * @param available   when true, only returns items that are currently orderable
     */
    @GetMapping("/api/menu")
    public ResponseEntity<List<MenuItemResponse>> getMenuItems(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean available) {
        return ResponseEntity.ok(menuService.getAllMenuItems(categoryId, available));
    }

    @GetMapping("/api/menu/{id}")
    public ResponseEntity<MenuItemResponse> getMenuItemById(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getMenuItemById(id));
    }
}
