package com.example.order_management.dto;

import com.example.order_management.entity.OrderStatus;
import com.example.order_management.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        List<OrderItemResponse> items,
        BigDecimal subtotalPrice,
        UUID discountId,
        BigDecimal discountValue,
        BigDecimal shippingFee,
        BigDecimal totalPrice,
        String recipientAddress,
        String paymentMethodName,
        Instant createdAt
) {
}
