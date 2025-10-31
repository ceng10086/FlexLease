package com.flexlease.order.repository;

import com.flexlease.order.domain.CartItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<CartItem> findByUserIdAndSkuId(UUID userId, UUID skuId);

    Optional<CartItem> findByIdAndUserId(UUID id, UUID userId);

    void deleteByUserId(UUID userId);

    void deleteByUserIdAndIdIn(UUID userId, List<UUID> ids);
}
