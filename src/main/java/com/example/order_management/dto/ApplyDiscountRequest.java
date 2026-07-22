package com.example.order_management.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApplyDiscountRequest(@NotNull UUID discountId) {
}
