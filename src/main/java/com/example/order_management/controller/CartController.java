package com.example.order_management.controller;

import com.example.order_management.common.BaseResponse;
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
    public ResponseEntity<BaseResponse<CartResponse>> getMyCart(@AuthenticationPrincipal CustomUserDetails principal) {
        UUID uuid = principal.user().getId();
        return ResponseEntity.ok(BaseResponse.success(cartService.getMyCart(uuid)));
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<BaseResponse<Void>> updateItemQuantity(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request
            ) {
        UUID userId = principal.user().getId();
        cartService.updateItemQuantity(userId, cartItemId, request.quantity());

        return ResponseEntity.ok(BaseResponse.success(null, "Cart Item Updated Successfully"));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<BaseResponse<Void>> removeItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID cartItemId
    ) {
        UUID userId = principal.user().getId();
        cartService.removeItem(userId, cartItemId);

        return ResponseEntity.ok(BaseResponse.success(null, "Cart Item Removed Successfully"));
    }

    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<OrderSummaryResponse>> getOrderSummary(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) UUID discountId
    ) {
        UUID userId = principal.user().getId();
        return ResponseEntity.ok(BaseResponse.success(cartService.getOrderSummary(userId, discountId)));
    }
}
