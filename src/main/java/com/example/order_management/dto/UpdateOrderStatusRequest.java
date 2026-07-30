package com.example.order_management.dto;

import com.example.order_management.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,
        String location,
        @Size(max = 500) String note
        ) {
}
