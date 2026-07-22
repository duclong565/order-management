package com.example.order_management.controller;

import com.example.order_management.dto.ApplyDiscountRequest;
import com.example.order_management.dto.CartResponse;
import com.example.order_management.dto.OrderSummaryResponse;
import com.example.order_management.dto.UpdateCartItemRequest;
import com.example.order_management.security.CustomUserDetails;
import com.example.order_management.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/me")
    public ResponseEntity<CartResponse> getMyCart(@AuthenticationPrincipal CustomUserDetails principal) {
        UUID uuid = principal.getUser().getId();
        return ResponseEntity.ok(cartService.getMyCart(uuid));
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request
            ) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, cartItemId, request.quantity()));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID cartItemId
    ) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(cartService.removeItem(userId, cartItemId));
    }

    @PatchMapping("/discount")
    public ResponseEntity<OrderSummaryResponse> applyDiscount(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ApplyDiscountRequest request
            ) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(cartService.applyDiscount(userId, request.discountId()));
    }

    @DeleteMapping("/discount")
    public ResponseEntity<OrderSummaryResponse> removeDiscount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(cartService.removeDiscount(userId));
    }

    @GetMapping("/summary")
    public ResponseEntity<OrderSummaryResponse> getOrderSummary(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(cartService.getOrderSummary(userId));
    }
}
