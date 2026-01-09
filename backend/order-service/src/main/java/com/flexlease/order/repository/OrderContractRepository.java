package com.flexlease.order.repository;

import com.flexlease.order.domain.OrderContract;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 订单合同仓库。
 */
public interface OrderContractRepository extends JpaRepository<OrderContract, UUID> {

    Optional<OrderContract> findByOrder_Id(UUID orderId);
}
