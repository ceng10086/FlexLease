package com.flexlease.product.repository;

import com.flexlease.product.domain.MediaAsset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    List<MediaAsset> findByProductIdOrderBySortOrderAscCreatedAtAsc(UUID productId);

    Optional<MediaAsset> findByIdAndProductId(UUID id, UUID productId);
}
