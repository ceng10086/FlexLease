package com.flexlease.product.dto;

import com.flexlease.product.domain.ProductStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 商品详情响应（包含媒体与租赁方案列表）。
 */
public record ProductResponse(
        UUID id,
        UUID vendorId,
        String name,
        String categoryCode,
        String description,
        String coverImageUrl,
        ProductStatus status,
        String reviewRemark,
        OffsetDateTime submittedAt,
        UUID reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<MediaAssetResponse> mediaAssets,
        List<RentalPlanResponse> rentalPlans
) {
}
