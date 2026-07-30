package com.example.order_management.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemRow(
        UUID cartItemId,
        UUID productVariantId,
        String productName,
        String variantName,
        BigDecimal unitPrice,
        int quantity,
        long stockQuantity
) {}

