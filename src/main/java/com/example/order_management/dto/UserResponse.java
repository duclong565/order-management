package com.example.order_management.dto;

import com.example.order_management.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        Instant createdAt
) {
}
