package com.flexlease.product.repository;

import com.flexlease.product.domain.Product;
import com.flexlease.product.domain.ProductStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 商品仓储（JPA）。
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByVendorId(UUID vendorId, Pageable pageable);

    Optional<Product> findByIdAndVendorId(UUID id, UUID vendorId);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByStatusAndCategoryCode(ProductStatus status, String categoryCode, Pageable pageable);

    Page<Product> findByVendorIdAndStatus(UUID vendorId, ProductStatus status, Pageable pageable);

    Page<Product> findByVendorIdAndNameContainingIgnoreCase(UUID vendorId, String keyword, Pageable pageable);

    Page<Product> findByVendorIdAndStatusAndNameContainingIgnoreCase(UUID vendorId, ProductStatus status, String keyword, Pageable pageable);

    Page<Product> findByStatusAndNameContainingIgnoreCase(ProductStatus status, String keyword, Pageable pageable);

    Page<Product> findByStatusAndCategoryCodeAndNameContainingIgnoreCase(ProductStatus status, String categoryCode, String keyword, Pageable pageable);

    /**
     * 预加载租赁方案、SKU 与媒体资源，避免 Web 层读取时触发 N+1。
     */
    @EntityGraph(attributePaths = {"rentalPlans", "rentalPlans.skus", "mediaAssets"})
    Optional<Product> findWithPlansById(UUID id);

    /**
     * 预加载租赁方案、SKU 与媒体资源，并限制为指定厂商的商品。
     */
    @EntityGraph(attributePaths = {"rentalPlans", "rentalPlans.skus", "mediaAssets"})
    Optional<Product> findWithPlansByIdAndVendorId(UUID id, UUID vendorId);
}
