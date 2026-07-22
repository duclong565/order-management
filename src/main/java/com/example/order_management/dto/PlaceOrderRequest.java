package com.example.order_management.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PlaceOrderRequest(
        @NotNull UUID recipientAddressId,
        @NotNull UUID paymentMethodId
        ) {
}
