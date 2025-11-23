package com.flexlease.order.repository;

import com.flexlease.order.domain.OrderDispute;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDisputeRepository extends JpaRepository<OrderDispute, UUID> {

    Optional<OrderDispute> findByIdAndOrderId(UUID id, UUID orderId);
}
