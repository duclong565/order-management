package com.example.order_management.service;

import com.example.order_management.dto.CartItemResponse;
import com.example.order_management.dto.CartResponse;
import com.example.order_management.dto.OrderSummaryResponse;
import com.example.order_management.entity.*;
import com.example.order_management.exception.BusinessException;
import com.example.order_management.exception.ResourceNotFoundException;
import com.example.order_management.pricing.PricingCalculator;
import com.example.order_management.repository.CartItemRepository;
import com.example.order_management.repository.CartRepository;
import com.example.order_management.repository.DiscountRepository;
import com.example.order_management.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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

    @Value("${app.shipping-fee}")
    private BigDecimal shippingFee;

    private Cart findCartByUserId(UUID userId) {
        return cartRepository.findByUserId(userId).orElseThrow(() ->
                new ResourceNotFoundException("Cart not found for user: " + userId));

    }

    private CartItemResponse toItemResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        BigDecimal lineTotal = variant.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponse(
                item.getId(),
                variant.getId(),
                variant.getProduct().getName(),
                variant.getName(),
                variant.getPrice(),
                item.getQuantity(),
                lineTotal
        );
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> cartItem = cartItemRepository.findByCartIdWithVariant(cart.getId());

        List<CartItemResponse> itemResponses = cartItem.stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal totalPrice = itemResponses.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), cart.getUser().getId(), itemResponses, totalPrice);
    }

    @Transactional(readOnly = true)
    public CartResponse getMyCart(UUID userId) {
        Cart cart = findCartByUserId(userId);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID userId, UUID cartItemId, Integer quantity) {

        CartItem cartItem = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        UUID variantId = cartItem.getProductVariant().getId();

        Inventory itemInventory = inventoryRepository.findByProductVariantId(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for variant: " + variantId));

        if (itemInventory.getQuantity() < quantity) {
            throw new BusinessException("Not enough stock. Available quantity: " + itemInventory.getQuantity() );
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        return getMyCart(userId);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID cartItemId) {
        CartItem cartItem = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));
        cartItemRepository.delete(cartItem);
        return getMyCart(userId);
    }

    @Transactional
    public OrderSummaryResponse applyDiscount(UUID userId, UUID discountId) {
        Cart cart = findCartByUserId(userId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found: " + discountId));

        Instant now = Instant.now();

        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
            throw new BusinessException("Discount is not active yet");
        }
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            throw new BusinessException("Discount has expired");
        }

        cart.setDiscount(discount);
        cartRepository.save(cart);
        return getOrderSummary(userId);
    }

    @Transactional
    public OrderSummaryResponse removeDiscount(UUID userId) {
        Cart cart = findCartByUserId(userId);

        cart.setDiscount(null);
        cartRepository.save(cart);
        return getOrderSummary(userId);
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse getOrderSummary(UUID userId) {
        Cart cart = findCartByUserId(userId);
        CartResponse cartResponse = buildCartResponse(cart);
        BigDecimal subtotal = cartResponse.totalAmount();

        Discount discount = cart.getDiscount();
        BigDecimal discountAmount = pricingCalculator.calculateDiscountAmount(discount, subtotal);
        BigDecimal totalPrice = subtotal.subtract(discountAmount).add(shippingFee);

        return new OrderSummaryResponse(
                cart.getId(),
                cartResponse.cartItems(),
                subtotal,
                discount != null ? discount.getId() : null,
                discount != null ? discount.getName() : null,
                discountAmount,
                shippingFee,
                totalPrice
        );
    }
}
