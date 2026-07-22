package com.example.order_management.controller;

import com.example.order_management.dto.OrderResponse;
import com.example.order_management.dto.PlaceOrderRequest;
import com.example.order_management.security.CustomUserDetails;
import com.example.order_management.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@AuthenticationPrincipal CustomUserDetails user,
                                                    @Valid @RequestBody PlaceOrderRequest request) {
        UUID userId = user.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(userId, request));
    }

}
