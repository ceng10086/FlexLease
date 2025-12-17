package com.flexlease.user.repository;

import com.flexlease.user.domain.CreditAdjustment;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditAdjustmentRepository extends JpaRepository<CreditAdjustment, UUID> {

    Page<CreditAdjustment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
