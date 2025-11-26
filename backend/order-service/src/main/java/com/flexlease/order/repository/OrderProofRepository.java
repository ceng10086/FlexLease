package com.flexlease.order.repository;

import com.flexlease.order.domain.OrderProof;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProofRepository extends JpaRepository<OrderProof, UUID> {

    List<OrderProof> findByOrderIdOrderByUploadedAtAsc(UUID orderId);

    Optional<OrderProof> findByIdAndOrderId(UUID id, UUID orderId);

    @EntityGraph(attributePaths = "order")
    Optional<OrderProof> findByFileName(String fileName);
}
