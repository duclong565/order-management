package com.example.order_management.service;

import com.example.order_management.common.ErrorCode;
import com.example.order_management.common.StockStatus;
import com.example.order_management.dto.CartItemResponse;
import com.example.order_management.dto.CartItemRow;
import com.example.order_management.dto.CartResponse;
import com.example.order_management.dto.OrderSummaryResponse;
import com.example.order_management.entity.*;
import com.example.order_management.exception.ApplicationException;
import com.example.order_management.pricing.PricingCalculator;
import com.example.order_management.repository.CartItemRepository;
import com.example.order_management.repository.CartRepository;
import com.example.order_management.repository.DiscountRepository;
import com.example.order_management.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final DiscountRepository discountRepository;
    private final PricingCalculator pricingCalculator;

    private Cart findCartByUserId(UUID userId) {
        return cartRepository.findByUserId(userId).orElseThrow(() ->
                new ApplicationException(ErrorCode.CART_NOT_FOUND));
    }

    private CartItemResponse toItemResponse(CartItemRow row) {
        StockStatus status = pricingCalculator.resolveStockStatus(row.getStockQuantity(), row.getQuantity());
        Integer available = status == StockStatus.LIMITED_STOCK ? (int) row.getStockQuantity() : null;

        return new CartItemResponse(
                row.getCartItemId(),
                row.getProductVariantId(),
                row.getProductName(),
                row.getVariantName(),
                row.getUnitPrice(),
                row.getQuantity(),
                status,
                available
        );
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository.findCartRows(cart.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new CartResponse(cart.getId(), cart.getUser().getId(), items);
    }

    @Transactional(readOnly = true)
    public CartResponse getMyCart(UUID userId) {
        Cart cart = findCartByUserId(userId);
        return buildCartResponse(cart);
    }

    @Transactional
    public void updateItemQuantity(UUID userId, UUID cartItemId, Integer quantity) {

        CartItem cartItem = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.CART_ITEM_NOT_FOUND));

        UUID variantId = cartItem.getProductVariant().getId();
        long availableStock = inventoryRepository.totalStock(variantId);

        if (availableStock < quantity) {
            throw new ApplicationException(ErrorCode.INSUFFICIENT_STOCK,
                    "Insufficient stock. Available quantity: " + availableStock);
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void removeItem(UUID userId, UUID cartItemId) {
        CartItem cartItem = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.CART_ITEM_NOT_FOUND));
        cartItemRepository.delete(cartItem);
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse getOrderSummary(UUID userId, UUID discountId) {
        //Get items for calculation
        Cart cart = findCartByUserId(userId);
        List<CartItem> cartItems = cartItemRepository.findByCartIdWithVariant(cart.getId());
        BigDecimal subtotal = pricingCalculator.calculateSubtotal(cartItems);

        Discount discount = null;
        if (discountId != null) {
            discount = discountRepository.findById(discountId)
                    .orElseThrow(() -> new ApplicationException(ErrorCode.DISCOUNT_NOT_FOUND));
            pricingCalculator.validateDiscountActive(discount);
        }

        BigDecimal shippingFee = pricingCalculator.getShippingFee();
        BigDecimal discountAmount = pricingCalculator.calculateDiscountAmount(discount, subtotal);
        BigDecimal totalPrice = subtotal.subtract(discountAmount).add(shippingFee);

        return new OrderSummaryResponse(
                subtotal,
                discountAmount,
                shippingFee,
                totalPrice
        );
    }
}
