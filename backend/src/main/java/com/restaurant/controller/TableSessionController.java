package com.restaurant.controller;

import com.restaurant.dto.order.PaySessionRequest;
import com.restaurant.dto.order.TableSessionResponse;
import com.restaurant.service.TableSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/table-sessions")
@RequiredArgsConstructor
public class TableSessionController {

    private final TableSessionService tableSessionService;

    @GetMapping("/{tableNumber}")
    public ResponseEntity<TableSessionResponse> getSession(@PathVariable String tableNumber) {
        return tableSessionService.getOpenSession(tableNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{tableNumber}/pay")
    public ResponseEntity<TableSessionResponse> paySession(
            @PathVariable String tableNumber,
            @Valid @RequestBody PaySessionRequest request) {
        return ResponseEntity.ok(tableSessionService.paySession(tableNumber, request));
    }
}
