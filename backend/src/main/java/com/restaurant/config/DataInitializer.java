package com.restaurant.config;

import com.restaurant.model.Category;
import com.restaurant.model.MenuItem;
import com.restaurant.model.User;
import com.restaurant.model.enums.Role;
import com.restaurant.repository.CategoryRepository;
import com.restaurant.repository.MenuItemRepository;
import com.restaurant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    public void run(String... args) {
        createDefaultUsers();
        createSampleData();
    }

    private void createDefaultUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .name("Admin")
                    .username("admin")
                    .email("admin@restaurant.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .phone("0800000001")
                    .build());
            log.info("Created default admin: admin / admin123");
        }

        if (!userRepository.existsByUsername("staff")) {
            userRepository.save(User.builder()
                    .name("Staff")
                    .username("staff")
                    .email("staff@restaurant.com")
                    .password(passwordEncoder.encode("staff123"))
                    .role(Role.STAFF)
                    .phone("0800000002")
                    .build());
            log.info("Created default staff: staff / staff123");
        }
    }

    private void createSampleData() {
        if (categoryRepository.count() > 0) return;

        Category appetizers = categoryRepository.save(Category.builder()
                .name("Appetizers").description("Start your meal right")
                .imageUrl("https://images.unsplash.com/photo-1541014741259-de529411b96a?w=400").build());

        Category mainCourse = categoryRepository.save(Category.builder()
                .name("Main Course").description("Hearty and satisfying mains")
                .imageUrl("https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400").build());

        Category desserts = categoryRepository.save(Category.builder()
                .name("Desserts").description("Sweet endings")
                .imageUrl("https://images.unsplash.com/photo-1551024506-0bccd828d307?w=400").build());

        Category beverages = categoryRepository.save(Category.builder()
                .name("Beverages").description("Refreshing drinks")
                .imageUrl("https://images.unsplash.com/photo-1544145945-f90425340c7e?w=400").build());

        // Appetizers
        menuItemRepository.save(MenuItem.builder().name("Spring Rolls").description("Crispy vegetable spring rolls served with sweet chili sauce").price(new BigDecimal("35000")).imageUrl("https://images.unsplash.com/photo-1617093727343-374698b1b08d?w=400").category(appetizers).available(true).build());
        menuItemRepository.save(MenuItem.builder().name("Caesar Salad").description("Romaine lettuce, parmesan, croutons with Caesar dressing").price(new BigDecimal("45000")).imageUrl("https://images.unsplash.com/photo-1550304943-4f24f54ddde9?w=400").category(appetizers).available(true).build());
        menuItemRepository.save(MenuItem.builder().name("Soup of the Day").description("Chef's daily soup selection served with bread").price(new BigDecimal("30000")).imageUrl("https://images.unsplash.com/photo-1547592180-85f173990554?w=400").category(appetizers).available(true).build());

        // Main Course
        menuItemRepository.save(MenuItem.builder().name("Grilled Salmon").description("Atlantic salmon fillet with lemon butter sauce and seasonal vegetables").price(new BigDecimal("120000")).imageUrl("https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=400").category(mainCourse).available(true).build());
        menuItemRepository.save(MenuItem.builder().name("Ribeye Steak").description("300g prime ribeye steak with mashed potatoes and mushroom sauce").price(new BigDecimal("185000")).imageUrl("https://images.unsplash.com/photo-1600891964092-4316c288032e?w=400").category(mainCourse).available(true).build());
        menuItemRepository.save(MenuItem.builder().name("Pasta Carbonara").description("Spaghetti with creamy egg sauce, pancetta and parmesan").price(new BigDecimal("75000")).imageUrl("https://images.unsplash.com/photo-1612874742237-6526221588e3?w=400").category(mainCourse).available(true).build());
        menuItemRepository.save(MenuItem.builder().name("Margherita Pizza").description("Classic tomato sauce, fresh mozzarella and basil").price(new BigDecimal("65000")).imageUrl("https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=400").category(mainCourse).available(true).build());

        // Desserts
        menuItemRepository.save(MenuItem.builder().name("Chocolate Lava Cake").description("Warm chocolate cake with molten center and vanilla ice cream").price(new BigDecimal("45000")).imageUrl("https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400").category(desserts).available(true).build());
        menuItemRepository.save(MenuItem.builder().name("Tiramisu").description("Classic Italian dessert with espresso and mascarpone").price(new BigDecimal("40000")).imageUrl("https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=400").category(desserts).available(true).build());

        // Beverages
        menuItemRepository.save(MenuItem.builder().name("Fresh Orange Juice").description("Freshly squeezed orange juice").price(new BigDecimal("25000")).imageUrl("https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=400").category(beverages).available(true).build());
        menuItemRepository.save(MenuItem.builder().name("Iced Coffee").description("Cold brew coffee over ice with milk").price(new BigDecimal("28000")).imageUrl("https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=400").category(beverages).available(true).build());
        menuItemRepository.save(MenuItem.builder().name("Sparkling Water").description("Chilled sparkling mineral water 500ml").price(new BigDecimal("15000")).imageUrl("https://images.unsplash.com/photo-1548839140-29a749e1cf4d?w=400").category(beverages).available(true).build());

        log.info("Sample menu data created.");
    }
}
