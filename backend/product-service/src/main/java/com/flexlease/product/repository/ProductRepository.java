package com.flexlease.product.repository;

import com.flexlease.product.domain.Product;
import com.flexlease.product.domain.ProductStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

    @EntityGraph(attributePaths = {"rentalPlans"})
    Optional<Product> findWithPlansById(UUID id);

    @EntityGraph(attributePaths = {"rentalPlans"})
    Optional<Product> findWithPlansByIdAndVendorId(UUID id, UUID vendorId);
}
