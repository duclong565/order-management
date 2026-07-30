package com.example.order_management.repository;

import com.example.order_management.entity.TrackingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TrackingLogRepository extends JpaRepository<TrackingLog, UUID> {

    @Query("""
            select tl from TrackingLog tl
            join fetch tl.user
            where tl.order.id = :orderId
            order by tl.createdAt asc
            """)
    List<TrackingLog> findByOrderIdWithUser(@Param("orderId") UUID orderId);
}
