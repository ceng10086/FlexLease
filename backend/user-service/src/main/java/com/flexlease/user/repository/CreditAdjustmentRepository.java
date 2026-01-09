package com.flexlease.user.repository;

import com.flexlease.user.domain.CreditAdjustment;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 信用分人工调整记录仓储（JPA）。
 */
public interface CreditAdjustmentRepository extends JpaRepository<CreditAdjustment, UUID> {

    Page<CreditAdjustment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
