package com.example.order_management.pricing;

import com.example.order_management.common.DiscountType;
import com.example.order_management.common.StockStatus;
import com.example.order_management.entity.Discount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test thuan - KHONG co Spring, khong co DB.
 * Chay trong vai mili giay.
 */
class PricingCalculatorTest {

    private PricingCalculator calculator;

    @BeforeEach
    void setUp() {
        // chay truoc MOI @Test -> moi test bat dau tu object sach
        calculator = new PricingCalculator(10, new BigDecimal("30000"));
    }

    private Discount discount(DiscountType type, String value) {
        Discount d = new Discount();
        d.setType(type);
        d.setValue(new BigDecimal(value));
        return d;
    }

    @Test
    @DisplayName("PERCENT: giam 10% cua 607000 -> 60700")
    void calculateDiscountAmount_percentType_shouldReturnPercentageOfSubtotal() {
        // Arrange
        Discount d = discount(DiscountType.PERCENT, "10");

        // Act
        BigDecimal result = calculator.calculateDiscountAmount(d, new BigDecimal("607000"));

        // Assert - BigDecimal PHAI dung isEqualByComparingTo (isEqualTo so ca scale)
        assertThat(result).isEqualByComparingTo("60700");
    }

    @Test
    @DisplayName("Khong chon ma -> khong giam gi")
    void calculateDiscountAmount_nullDiscount_shouldReturnZero() {
        BigDecimal result = calculator.calculateDiscountAmount(null, new BigDecimal("607000"));

        assertThat(result).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Giam 50k")
    void calculateDiscountAmount_fixedType_shouldReturnFixedValue() {
        Discount d = discount(DiscountType.FIXED, "50000");

        BigDecimal result = calculator.calculateDiscountAmount(d, new BigDecimal("607000"));

        assertThat(result).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("Giam 1tr dong")
    void calculateDiscountAmount_whenDiscountExceedsSubtotal_shouldCapAtSubtotal() {
        Discount d = discount(DiscountType.FIXED, "1000000");

        BigDecimal result = calculator.calculateDiscountAmount(d, new BigDecimal("607000"));

        assertThat(result).isEqualByComparingTo("607000");
    }

    @Test
    @DisplayName("Giam 10%")
    void calculateDiscountAmount_whenDiscountIsPercent_shouldReturnDiscountAmount() {
        Discount d = discount(DiscountType.PERCENT, "10");
        BigDecimal result = calculator.calculateDiscountAmount(d, new BigDecimal("100"));
        assertThat(result).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("PERCENT: 10% cua 999 = 99.9 -> lam tron HALF_UP thanh 100")
    void calculateDiscountAmount_whenResultHasFraction_shouldRoundHalfUp() {
        Discount d = discount(DiscountType.PERCENT, "10");

        BigDecimal result = calculator.calculateDiscountAmount(d, new BigDecimal("999"));

        // 99.9 -> HALF_UP -> 100 (VND khong co xu)
        assertThat(result).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Ton kho 0 -> OUT_OF_STOCK")
    void resolveStockStatus_whenNoStock_shouldReturnOutOfStock() {
        StockStatus status = calculator.resolveStockStatus(0, 1);

        assertThat(status).isEqualTo(StockStatus.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("Ton kho 5 (<= nguong 10), muon mua 3 -> LIMITED_STOCK")
    void resolveStockStatus_whenStockBelowThreshold_shouldReturnLimitedStock() {
        StockStatus status = calculator.resolveStockStatus(5, 3);

        assertThat(status).isEqualTo(StockStatus.LIMITED_STOCK);
    }

    @Test
    @DisplayName("Ton kho 100 (> nguong 10) -> IN_STOCK")
    void resolveStockStatus_whenStockAboveThreshold_shouldReturnInStock() {
        StockStatus status = calculator.resolveStockStatus(100, 2);

        assertThat(status).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    @DisplayName("Con 5 nhung muon mua 10 -> OUT_OF_STOCK (khong du)")
    void resolveStockStatus_whenWantedExceedsStock_shouldReturnOutOfStock() {
        StockStatus status = calculator.resolveStockStatus(5, 10);

        assertThat(status).isEqualTo(StockStatus.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("Ton kho dung bang nguong 10 -> van LIMITED_STOCK (bien)")
    void resolveStockStatus_whenStockEqualsThreshold_shouldReturnLimitedStock() {
        StockStatus status = calculator.resolveStockStatus(10, 1);

        assertThat(status).isEqualTo(StockStatus.LIMITED_STOCK);
    }
}
