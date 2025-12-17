package com.flexlease.product.repository;

import com.flexlease.product.domain.ProductInquiry;
import com.flexlease.product.domain.ProductInquiryStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductInquiryRepository extends JpaRepository<ProductInquiry, UUID> {

    @Query("""
            select pi from ProductInquiry pi
            where pi.vendorId = :vendorId
              and (:status is null or pi.status = :status)
            order by pi.createdAt desc
            """)
    List<ProductInquiry> findByVendor(@Param("vendorId") UUID vendorId,
                                      @Param("status") ProductInquiryStatus status);

    @Query("""
            select pi from ProductInquiry pi
            where pi.product.id = :productId
              and pi.requesterId = :requesterId
            order by pi.createdAt desc
            """)
    List<ProductInquiry> findByRequester(@Param("productId") UUID productId,
                                         @Param("requesterId") UUID requesterId);

    Optional<ProductInquiry> findByIdAndVendorId(UUID id, UUID vendorId);

    List<ProductInquiry> findByStatusAndExpiresAtBefore(ProductInquiryStatus status, OffsetDateTime cutoff);
}
