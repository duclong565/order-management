package com.example.order_management.repository;

import com.example.order_management.entity.Cart;
import com.example.order_management.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    @Query("""
        select ci from CartItem ci
        join fetch ci.productVariant v
        join fetch v.product
        where ci.cart.id = :cartId
        """)
    List<CartItem> findByCartIdWithVariant(@Param("cartId") UUID cartId);

    Optional<CartItem> findByIdAndCartUserId(UUID id, UUID userId);
}
