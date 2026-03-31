package com.restaurant.controller;

import com.restaurant.dto.order.OrderRequest;
import com.restaurant.dto.order.OrderResponse;
import com.restaurant.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(request));
    }

    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<OrderResponse> trackOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }

    @PostMapping("/{orderNumber}/pay")
    public ResponseEntity<OrderResponse> confirmPayment(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.confirmPayment(orderNumber));
    }
}
