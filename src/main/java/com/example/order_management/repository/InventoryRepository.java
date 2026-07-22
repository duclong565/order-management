package com.example.order_management.repository;

import com.example.order_management.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductVariantId(UUID productVariantId);

    @Modifying
    @Query(
            """
        update Inventory i
        set i.quantity = i.quantity - :qty
        where i.productVariant.id = :variantId
          and i.quantity >= :qty
        """
    )
    int decreaseStock(@Param("variantId") UUID variantId, @Param("qty") int qty);
}
