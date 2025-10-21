package com.flexlease.product.repository;

import com.flexlease.product.domain.ProductSku;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSkuRepository extends JpaRepository<ProductSku, UUID> {

    Optional<ProductSku> findByIdAndProductId(UUID id, UUID productId);

    Optional<ProductSku> findBySkuCodeIgnoreCase(String skuCode);
}
