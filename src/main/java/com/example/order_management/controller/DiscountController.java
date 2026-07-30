package com.example.order_management.controller;

import com.example.order_management.common.BaseResponse;
import com.example.order_management.dto.DiscountResponse;
import com.example.order_management.service.DiscountService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/discounts")
@AllArgsConstructor
public class DiscountController {

    private final DiscountService  discountService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<DiscountResponse>>> getActiveDiscounts() {
        return ResponseEntity.ok(BaseResponse.success(discountService.getActiveDiscounts()));
    }
}
