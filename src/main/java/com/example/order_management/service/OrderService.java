package com.example.order_management.service;

import com.example.order_management.dto.*;
import com.example.order_management.entity.*;
import com.example.order_management.common.OrderStatus;
import com.example.order_management.common.PaymentStatus;
import com.example.order_management.common.ErrorCode;
import com.example.order_management.exception.ApplicationException;
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
    private final TrackingLogRepository trackingLogRepository;
    private final UserRepository userRepository;
    private final DiscountRepository discountRepository;

    private void decreaseStock(List<CartItem> items) {
        for (CartItem item : items) {
            ProductVariant variant = item.getProductVariant();
            int updated = inventoryRepository.decreaseStock(variant.getId(), item.getQuantity());

            if (updated == 0) {
                throw new ApplicationException(ErrorCode.INSUFFICIENT_STOCK,
                        "Insufficient stock for variant: " + variant.getName());
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
                .orElseThrow(() -> new ApplicationException(ErrorCode.CART_NOT_FOUND));

        List<CartItem> items = cartItemRepository.findByCartIdWithVariant(cart.getId());
        if (items.isEmpty()) {
            throw new ApplicationException(ErrorCode.CART_EMPTY);
        }

        Address address = addressRepository.findByIdAndUserId(request.recipientAddressId(), userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ADDRESS_NOT_FOUND));

        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.paymentMethodId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        decreaseStock(items);

        BigDecimal subtotal = items.stream()
                .map(item -> item.getProductVariant().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Discount discount = null;
        if (request.discountId() != null) {
            discount = discountRepository.findById(request.discountId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.DISCOUNT_NOT_FOUND));
            pricingCalculator.validateDiscountActive(discount);
        }

        BigDecimal discountAmount = pricingCalculator.calculateDiscountAmount(discount, subtotal);
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

    @Transactional(readOnly = true)
    public List<OrderListItemResponse> getMyOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(o -> new OrderListItemResponse(
                        o.getId(),
                        o.getStatus(),
                        o.getPaymentStatus(),
                        o.getTotalPrice(),
                        o.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderItem> items = orderItemRepository.findByOrderIdWithVariant(orderId);

        return toResponse(order, items, order.getPaymentMethod());
    }

    @Transactional
    public OrderResponse updateStatus (UUID actorUserId, UUID orderId,
                                       UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus current = order.getStatus();
        OrderStatus target = request.status();

        if (!current.canTransitionTo(target)) {
            throw new ApplicationException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Invalid transition: " + current + " to " + target);
        }

        order.setStatus(target);

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        TrackingLog log = new TrackingLog();
        log.setOrder(order);
        log.setUser(actor);
        log.setStatus(target);
        log.setLocation(request.location());
        log.setNote(request.note());
        trackingLogRepository.save(log);

        List<OrderItem> items = orderItemRepository.findByOrderIdWithVariant(orderId);
        return toResponse(order, items, order.getPaymentMethod());
    }
}
