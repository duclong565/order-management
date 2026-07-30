package com.example.order_management.controller;

import com.example.order_management.common.BaseResponse;
import com.example.order_management.dto.OrderListItemResponse;
import com.example.order_management.dto.OrderResponse;
import com.example.order_management.dto.PlaceOrderRequest;
import com.example.order_management.security.CustomUserDetails;
import com.example.order_management.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<BaseResponse<OrderResponse>> placeOrder(@AuthenticationPrincipal CustomUserDetails user,
                                                    @Valid @RequestBody PlaceOrderRequest request) {
        UUID userId = user.user().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(orderService.placeOrder(userId, request)));
    }


    @GetMapping
    public ResponseEntity<BaseResponse<List<OrderListItemResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(orderService.getMyOrders(principal.user().getId())));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<BaseResponse<OrderResponse>> getMyOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(orderService.getMyOrder(
                        principal.user().getId(),
                        orderId
                )));
    }
}
