package com.example.order_management.dto;

import com.example.order_management.common.OrderStatus;
import com.example.order_management.common.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderListItemResponse(
        UUID orderId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalPrice,
        Instant createdAt
) {
}
