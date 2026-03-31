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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TableSessionControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MenuItemRepository menuItemRepository;
    @Autowired CategoryRepository categoryRepository;

    private Long menuItemId;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(
                Category.builder().name("Drinks").build()
        );
        MenuItem item = menuItemRepository.save(
                MenuItem.builder()
                        .name("Es Teh")
                        .price(BigDecimal.valueOf(10000))
                        .available(true)
                        .category(category)
                        .build()
        );
        menuItemId = item.getId();
    }

    // ── GET /api/table-sessions/{tableNumber} ─────────────────────────────────

    @Test
    void getSession_returnsNotFound_whenNoActiveSession() throws Exception {
        mockMvc.perform(get("/api/table-sessions/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSession_returnsSession_afterOrderPlaced() throws Exception {
        placeOrder("10", 1);

        mockMvc.perform(get("/api/table-sessions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableNumber").value("10"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.orderCount").value(1))
                .andExpect(jsonPath("$.totalAmount").value(10000));
    }

    @Test
    void getSession_accumulatesMultipleOrders() throws Exception {
        placeOrder("11", 1);
        placeOrder("11", 2);

        mockMvc.perform(get("/api/table-sessions/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(30000)); // 1×10000 + 2×10000
    }

    @Test
    void getSession_differentTables_areIsolated() throws Exception {
        placeOrder("12", 1);
        placeOrder("13", 2);

        // Table 12 has 1 order
        mockMvc.perform(get("/api/table-sessions/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(1))
                .andExpect(jsonPath("$.totalAmount").value(10000));

        // Table 13 has 1 order (different session)
        mockMvc.perform(get("/api/table-sessions/13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(1))
                .andExpect(jsonPath("$.totalAmount").value(20000));
    }

    // ── POST /api/table-sessions/{tableNumber}/pay ────────────────────────────

    @Test
    void paySession_markSessionAsPaid() throws Exception {
        placeOrder("14", 1);

        mockMvc.perform(post("/api/table-sessions/14/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"CASH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"));
    }

    @Test
    void paySession_noActiveSession_returns400() throws Exception {
        mockMvc.perform(post("/api/table-sessions/99/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"CASH\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paySession_createsNewSession_forSubsequentOrders() throws Exception {
        // First session: place order and pay
        placeOrder("15", 1);

        mockMvc.perform(post("/api/table-sessions/15/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"CASH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Session is now PAID — no active session
        mockMvc.perform(get("/api/table-sessions/15"))
                .andExpect(status().isNotFound());

        // New customers sit down, place a new order — new session starts
        placeOrder("15", 1);

        mockMvc.perform(get("/api/table-sessions/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.orderCount").value(1));
    }

    @Test
    void paySession_withCard_recordsPaymentMethod() throws Exception {
        placeOrder("16", 1);

        mockMvc.perform(post("/api/table-sessions/16/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"CARD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void placeOrder(String tableNumber, int quantity) throws Exception {
        String body = """
                {
                  "items": [{"menuItemId": %d, "quantity": %d, "notes": ""}],
                  "tableNumber": "%s",
                  "paymentMethod": "CASH"
                }
                """.formatted(menuItemId, quantity, tableNumber);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
