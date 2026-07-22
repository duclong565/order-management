package com.example.order_management.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID orderItemId,
        UUID productVariantId,
        String productName,
        String variantName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
