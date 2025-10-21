package com.flexlease.product.repository;

import com.flexlease.product.domain.RentalPlan;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalPlanRepository extends JpaRepository<RentalPlan, UUID> {

    Optional<RentalPlan> findByIdAndProductId(UUID id, UUID productId);
}
