package com.example.order_management.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        UUID userId,
        List<CartItemResponse> cartItems,
        BigDecimal totalAmount
) {}
