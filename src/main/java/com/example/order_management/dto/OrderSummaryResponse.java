package com.example.order_management.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID cartId,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        UUID discountId,
        String discountName,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal totalPrice
) {
}
