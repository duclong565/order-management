package com.example.order_management.dto;

import java.time.Instant;

public record ErrorResponse(int status, String message, Instant timestamp) {}