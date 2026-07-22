package com.example.order_management.pricing;

import com.example.order_management.entity.Discount;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.example.order_management.entity.DiscountType.FIXED;
import static com.example.order_management.entity.DiscountType.PERCENT;

@Getter
@Component
public class PricingCalculator {

    @Value("${app.shipping-fee}")
    private BigDecimal shippingFee;

    public BigDecimal calculateDiscountAmount(Discount discount, BigDecimal subtotal) {
        if (discount == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal amount = switch (discount.getType()) {
            case PERCENT -> subtotal.multiply(discount.getValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            case FIXED -> discount.getValue();
        };
        return amount.compareTo(subtotal) > 0 ? subtotal : amount;
    }
}
