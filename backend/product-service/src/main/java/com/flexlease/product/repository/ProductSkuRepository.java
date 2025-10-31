package com.flexlease.product.repository;

import com.flexlease.product.domain.ProductSku;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSkuRepository extends JpaRepository<ProductSku, UUID> {

    Optional<ProductSku> findByIdAndProductId(UUID id, UUID productId);

    Optional<ProductSku> findBySkuCodeIgnoreCase(String skuCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ProductSku s where s.id = :id")
    Optional<ProductSku> findByIdForUpdate(@Param("id") UUID id);
}
