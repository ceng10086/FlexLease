package com.flexlease.product.repository;

import com.flexlease.product.domain.InventorySnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 库存流水快照仓储（JPA）。
 */
public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, Long> {

    void deleteBySkuId(UUID skuId);
}
