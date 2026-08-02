package com.example.order_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.order_management.common.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResponse {

    private UUID id;
    private String name;
    private String description;
    private DiscountType type;
    private BigDecimal value;
    private Instant startDate;
    private Instant endDate;
}
