package com.flexlease.order.repository;

import com.flexlease.order.domain.OrderReturnRequest;
import com.flexlease.order.domain.ReturnRequestStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 退租申请仓库。
 */
public interface OrderReturnRequestRepository extends JpaRepository<OrderReturnRequest, UUID> {

    Optional<OrderReturnRequest> findFirstByOrderIdAndStatusOrderByRequestedAtDesc(UUID orderId, ReturnRequestStatus status);
}
