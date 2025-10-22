package com.flexlease.order.repository;

import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.domain.OrderStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalOrderRepository extends JpaRepository<RentalOrder, UUID> {

    Optional<RentalOrder> findWithDetailsById(UUID id);

    Page<RentalOrder> findByUserId(UUID userId, Pageable pageable);

    Page<RentalOrder> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Page<RentalOrder> findByVendorId(UUID vendorId, Pageable pageable);

    Page<RentalOrder> findByVendorIdAndStatus(UUID vendorId, OrderStatus status, Pageable pageable);
}
