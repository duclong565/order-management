package com.example.order_management.dto;

import com.example.order_management.common.OrderStatus;
import com.example.order_management.common.PaymentStatus;

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
