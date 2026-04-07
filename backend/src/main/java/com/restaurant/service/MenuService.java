package com.restaurant.service;

import com.restaurant.dto.menu.CategoryRequest;
import com.restaurant.dto.menu.CategoryResponse;
import com.restaurant.dto.menu.MenuItemRequest;
import com.restaurant.dto.menu.MenuItemResponse;
import com.restaurant.model.Category;
import com.restaurant.model.MenuItem;
import com.restaurant.repository.CategoryRepository;
import com.restaurant.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;

    // ── Categories ──────────────────────────────────────────────────────────

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::mapCategory).toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(this::mapCategory)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .build();
        CategoryResponse response = mapCategory(categoryRepository.save(category));
        log.info("Category created: id={}, name={}", response.id(), response.name());
        return response;
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        CategoryResponse response = mapCategory(categoryRepository.save(category));
        log.info("Category updated: id={}, name={}", id, response.name());
        return response;
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found");
        }
        categoryRepository.deleteById(id);
        log.info("Category deleted: id={}", id);
    }

    // ── Menu Items ───────────────────────────────────────────────────────────

    public List<MenuItemResponse> getAllMenuItems(Long categoryId, Boolean availableOnly) {
        List<MenuItem> items;

        if (categoryId != null && Boolean.TRUE.equals(availableOnly)) {
            items = menuItemRepository.findByCategoryIdAndAvailableTrue(categoryId);
        } else if (categoryId != null) {
            items = menuItemRepository.findByCategoryId(categoryId);
        } else if (Boolean.TRUE.equals(availableOnly)) {
            items = menuItemRepository.findByAvailableTrue();
        } else {
            items = menuItemRepository.findAll();
        }

        return items.stream().map(this::mapMenuItem).toList();
    }

    public MenuItemResponse getMenuItemById(Long id) {
        return menuItemRepository.findById(id)
                .map(this::mapMenuItem)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
    }

    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        MenuItem item = MenuItem.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .imageUrl(request.imageUrl())
                .category(category)
                .available(request.available())
                .build();

        MenuItemResponse response = mapMenuItem(menuItemRepository.save(item));
        log.info("MenuItem created: id={}, name={}", response.id(), response.name());
        return response;
    }

    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setImageUrl(request.imageUrl());
        item.setCategory(category);
        item.setAvailable(request.available());

        MenuItemResponse response = mapMenuItem(menuItemRepository.save(item));
        log.info("MenuItem updated: id={}, name={}", id, response.name());
        return response;
    }

    public void deleteMenuItem(Long id) {
        if (!menuItemRepository.existsById(id)) {
            throw new RuntimeException("Menu item not found");
        }
        menuItemRepository.deleteById(id);
        log.info("MenuItem deleted: id={}", id);
    }

    public MenuItemResponse toggleAvailability(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        item.setAvailable(!item.isAvailable());
        MenuItemResponse response = mapMenuItem(menuItemRepository.save(item));
        log.info("MenuItem availability toggled: id={}, available={}", id, response.available());
        return response;
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private CategoryResponse mapCategory(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getImageUrl());
    }

    public MenuItemResponse mapMenuItem(MenuItem m) {
        CategoryResponse cat = m.getCategory() != null ? mapCategory(m.getCategory()) : null;
        return new MenuItemResponse(m.getId(), m.getName(), m.getDescription(), m.getPrice(),
                m.getImageUrl(), cat, m.isAvailable());
    }
}
