package com.example.order_management.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID cartItemId,
        UUID productVariantId,
        String productName,
        String variantName,
        BigDecimal price,
        int quantity,
        BigDecimal lineTotal
) {}
