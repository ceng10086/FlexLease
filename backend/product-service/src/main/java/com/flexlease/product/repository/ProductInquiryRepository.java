package com.flexlease.product.repository;

import com.flexlease.product.domain.ProductInquiry;
import com.flexlease.product.domain.ProductInquiryStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 商品咨询仓储（JPA）。
 */
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

    /**
     * 批量将已超时但仍为 OPEN 的咨询置为 EXPIRED（用于定时任务）。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ProductInquiry pi
               set pi.status = :expired
             where pi.status = :open
               and pi.expiresAt < :cutoff
            """)
    int bulkExpire(@Param("open") ProductInquiryStatus open,
                   @Param("expired") ProductInquiryStatus expired,
                   @Param("cutoff") OffsetDateTime cutoff);
}
