package com.restaurant.controller;

import com.restaurant.dto.order.PaySessionRequest;
import com.restaurant.dto.order.TableSessionResponse;
import com.restaurant.service.TableSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoints for table session lookup and payment.
 * No authentication required — customers interact with these from the bill page.
 */
@RestController
@RequestMapping("/api/table-sessions")
@RequiredArgsConstructor
public class TableSessionController {

    private final TableSessionService tableSessionService;

    /**
     * Returns the active (OPEN) session for a table.
     * Returns 404 if no open session exists — the frontend treats this as
     * "session ended" and clears the customer's stored order history.
     */
    @GetMapping("/{tableNumber}")
    public ResponseEntity<TableSessionResponse> getSession(@PathVariable String tableNumber) {
        return tableSessionService.getOpenSession(tableNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Settles the entire table bill in one action.
     * Marks all pending payments as PAID and closes the session.
     */
    @PostMapping("/{tableNumber}/pay")
    public ResponseEntity<TableSessionResponse> paySession(
            @PathVariable String tableNumber,
            @Valid @RequestBody PaySessionRequest request) {
        return ResponseEntity.ok(tableSessionService.paySession(tableNumber, request));
    }
}
