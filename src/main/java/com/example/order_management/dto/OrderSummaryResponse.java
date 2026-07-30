package com.example.order_management.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderSummaryResponse(
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal totalPrice
) {
}
