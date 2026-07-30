package com.example.order_management.dto;

import com.example.order_management.common.UserRole;
import jakarta.validation.constraints.*;

//chuyển thành class
public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 255) String password,
        @NotNull UserRole role
) {}