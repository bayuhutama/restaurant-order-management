package com.restaurant.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.model.Category;
import com.restaurant.model.MenuItem;
import com.restaurant.repository.CategoryRepository;
import com.restaurant.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MenuItemRepository menuItemRepository;
    @Autowired CategoryRepository categoryRepository;

    private Long menuItemId;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(
                Category.builder().name("Main Course").build()
        );
        MenuItem item = menuItemRepository.save(
                MenuItem.builder()
                        .name("Nasi Goreng")
                        .price(BigDecimal.valueOf(35000))
                        .available(true)
                        .category(category)
                        .build()
        );
        menuItemId = item.getId();
    }

    // ── Place Order (guest) ───────────────────────────────────────────────────

    @Test
    void placeOrder_asGuest_returnsOrderInPendingState() throws Exception {
        String body = """
                {
                  "items": [{"menuItemId": %d, "quantity": 2, "notes": ""}],
                  "notes": "no spice",
                  "tableNumber": "5",
                  "guestName": "Alice",
                  "guestPhone": "08111111111",
                  "paymentMethod": "CASH"
                }
                """.formatted(menuItemId);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.tableNumber").value("5"))
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.totalAmount").value(70000));
    }

    @Test
    void placeOrder_multipleFromSameTable_createsSeparateOrdersInSameSession() throws Exception {
        String body1 = """
                {
                  "items": [{"menuItemId": %d, "quantity": 1, "notes": ""}],
                  "tableNumber": "7",
                  "guestName": "Alice",
                  "paymentMethod": "CASH"
                }
                """.formatted(menuItemId);

        String body2 = """
                {
                  "items": [{"menuItemId": %d, "quantity": 1, "notes": ""}],
                  "tableNumber": "7",
                  "guestName": "Bob",
                  "paymentMethod": "CASH"
                }
                """.formatted(menuItemId);

        // Both orders placed for same table
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableNumber").value("7"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableNumber").value("7"));

        // The table session should have 2 orders totaling 70000
        mockMvc.perform(get("/api/table-sessions/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(70000));
    }

    @Test
    void placeOrder_missingTableNumber_returns400() throws Exception {
        String body = """
                {
                  "items": [{"menuItemId": %d, "quantity": 1, "notes": ""}],
                  "paymentMethod": "CASH"
                }
                """.formatted(menuItemId);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_emptyItems_returns400() throws Exception {
        String body = """
                {
                  "items": [],
                  "tableNumber": "5",
                  "paymentMethod": "CASH"
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_nonExistentMenuItem_returns400() throws Exception {
        String body = """
                {
                  "items": [{"menuItemId": 99999, "quantity": 1, "notes": ""}],
                  "tableNumber": "5",
                  "paymentMethod": "CASH"
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── Track Order ───────────────────────────────────────────────────────────

    @Test
    void trackOrder_returnsOrderDetails() throws Exception {
        String placeBody = """
                {
                  "items": [{"menuItemId": %d, "quantity": 1, "notes": ""}],
                  "tableNumber": "8",
                  "guestName": "Carol",
                  "paymentMethod": "CASH"
                }
                """.formatted(menuItemId);

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeBody))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String orderNumber = (String) body.get("orderNumber");

        mockMvc.perform(get("/api/orders/track/" + orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void trackOrder_unknownOrderNumber_returns400() throws Exception {
        mockMvc.perform(get("/api/orders/track/ORD-INVALID-000000"))
                .andExpect(status().isBadRequest());
    }

    // ── Admin: update order status ────────────────────────────────────────────

    @Test
    void updateOrderStatus_asAdmin_changesStatus() throws Exception {
        // Place an order first
        String placeBody = """
                {
                  "items": [{"menuItemId": %d, "quantity": 1, "notes": ""}],
                  "tableNumber": "9",
                  "paymentMethod": "CASH"
                }
                """.formatted(menuItemId);

        MvcResult placed = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeBody))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> placedBody = objectMapper.readValue(placed.getResponse().getContentAsString(), Map.class);
        Long orderId = ((Number) placedBody.get("id")).longValue();

        // Get admin JWT
        String adminToken = loginAs("admin", "Admin123!");

        // Update status to CONFIRMED
        mockMvc.perform(patch("/api/staff/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateOrderStatus_withoutToken_returns403() throws Exception {
        mockMvc.perform(patch("/api/staff/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isForbidden());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String loginAs(String username, String password) throws Exception {
        String body = """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) response.get("token");
    }
}
