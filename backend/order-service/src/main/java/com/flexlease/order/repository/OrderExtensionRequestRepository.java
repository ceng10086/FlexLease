package com.flexlease.order.repository;

import com.flexlease.order.domain.ExtensionRequestStatus;
import com.flexlease.order.domain.OrderExtensionRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderExtensionRequestRepository extends JpaRepository<OrderExtensionRequest, UUID> {

    Optional<OrderExtensionRequest> findFirstByOrderIdAndStatusOrderByRequestedAtDesc(UUID orderId, ExtensionRequestStatus status);
}
