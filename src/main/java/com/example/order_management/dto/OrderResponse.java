package com.example.order_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.order_management.common.OrderStatus;
import com.example.order_management.common.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID orderId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private List<OrderItemResponse> items;
    private BigDecimal subtotalPrice;
    private UUID discountId;
    private BigDecimal discountValue;
    private BigDecimal shippingFee;
    private BigDecimal totalPrice;
    private String recipientAddress;
    private String paymentMethodName;
    private Instant createdAt;
}
