package com.example.order_management.service;

import com.example.order_management.dto.OrderItemResponse;
import com.example.order_management.dto.OrderResponse;
import com.example.order_management.dto.PlaceOrderRequest;
import com.example.order_management.entity.*;
import com.example.order_management.exception.BusinessException;
import com.example.order_management.exception.ResourceNotFoundException;
import com.example.order_management.pricing.PricingCalculator;
import com.example.order_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final AddressRepository addressRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PricingCalculator  pricingCalculator;

    private void decreaseStock(List<CartItem> items) {
        for (CartItem item : items) {
            ProductVariant variant = item.getProductVariant();
            int updated = inventoryRepository.decreaseStock(variant.getId(), item.getQuantity());

            if (updated == 0) {
                throw new BusinessException("Insufficient stock for variant: " + variant.getName());
            }
        }
    }

    private OrderItem toOrderItem(Order order, CartItem cartItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductVariant(cartItem.getProductVariant());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setUnitPrice(cartItem.getProductVariant().getPrice());
        return orderItem;
    }

    private OrderResponse toResponse(Order order, List<OrderItem> orderItems, PaymentMethod paymentMethod) {
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getPaymentStatus(),
                itemResponses,
                order.getSubtotalPrice(),
                order.getDiscount() != null ? order.getDiscount().getId() : null,
                order.getDiscountValue(),
                order.getShippingFee(),
                order.getTotalPrice(),
                order.getRecipientAddressSnapshot(),
                paymentMethod.getName(),
                order.getCreatedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem orderItem) {
        ProductVariant productVariant = orderItem.getProductVariant();
        BigDecimal lineTotal = orderItem.getUnitPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

        return new OrderItemResponse(
                orderItem.getId(),
                productVariant.getId(),
                productVariant.getProduct().getName(),
                productVariant.getName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                lineTotal
        );
    }

    private String formatAddress(Address address) {
        return Stream.of(address.getLine1(), address.getLine2(), address.getCity(),
                address.getState(), address.getZipCode(), address.getCountry())
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(", "));
    }

    @Transactional
    public OrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
        Cart cart = cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        List<CartItem> items = cartItemRepository.findByCartIdWithVariant(cart.getId());
        if (items.isEmpty()) throw new BusinessException("Cart is empty for user: " + cart.getUser().getUsername());

        Address address = addressRepository.findByIdAndUserId(request.recipientAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found for user: " + request.recipientAddressId()));

        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.paymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + request.paymentMethodId()));

        decreaseStock(items);

        BigDecimal subtotal = items.stream()
                .map(item -> item.getProductVariant().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Discount discount = cart.getDiscount();
        BigDecimal discountAmount =
                pricingCalculator.calculateDiscountAmount(discount, subtotal);
        BigDecimal shippingFee = pricingCalculator.getShippingFee();
        BigDecimal totalPrice = subtotal.subtract(discountAmount).add(shippingFee);

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setDiscount(discount);
        order.setDiscountValue(discountAmount);
        order.setSubtotalPrice(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotalPrice(totalPrice);
        order.setPaymentMethod(paymentMethod);
        order.setRecipientAddress(address);
        order.setRecipientAddressSnapshot(formatAddress(address));
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = items.stream()
                .map(item -> toOrderItem(savedOrder, item))
                .toList();
        orderItemRepository.saveAll(orderItems);

        cartItemRepository.deleteAll(items);
        cart.setDiscount(null);
        cartRepository.save(cart);

        return toResponse(savedOrder, orderItems, paymentMethod);
    }
}
