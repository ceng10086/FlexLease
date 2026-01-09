package com.flexlease.product.repository;

import com.flexlease.product.domain.ProductSku;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 商品 SKU 仓储（JPA）。
 */
public interface ProductSkuRepository extends JpaRepository<ProductSku, UUID> {

    Optional<ProductSku> findByIdAndProductId(UUID id, UUID productId);

    Optional<ProductSku> findBySkuCodeIgnoreCase(String skuCode);
}
