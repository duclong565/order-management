package com.example.order_management.dto;

import com.example.order_management.entity.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscountResponse(
        UUID id,
        String name,
        String description,
        DiscountType type,
        BigDecimal value,
        Instant startDate,
        Instant endDate
) {
}
