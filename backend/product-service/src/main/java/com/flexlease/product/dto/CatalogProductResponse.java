package com.flexlease.product.dto;

import com.flexlease.product.domain.ProductStatus;
import com.flexlease.product.domain.RentalPlanType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 前台目录商品详情响应（面向消费者展示）。
 */
public record CatalogProductResponse(
        UUID id,
        UUID vendorId,
        String name,
        String categoryCode,
        String description,
        String coverImageUrl,
        ProductStatus status,
        List<MediaAssetItem> mediaAssets,
        List<RentalPlanItem> rentalPlans
) {

    /**
     * 媒体条目（仅暴露必要字段用于前台展示）。
     */
    public record MediaAssetItem(
            UUID id,
            String fileUrl,
            Integer sortOrder
    ) {}

    /**
     * 租赁方案条目（含 SKU 列表，用于前台选择规格/方案）。
     */
    public record RentalPlanItem(
            UUID id,
            RentalPlanType planType,
            int termMonths,
            BigDecimal depositAmount,
            BigDecimal rentAmountMonthly,
            BigDecimal buyoutPrice,
            boolean allowExtend,
            String extensionUnit,
            BigDecimal extensionPrice,
            List<CatalogSkuItem> skus
    ) {}

    /**
     * SKU 条目（前台目录使用）。
     */
    public record CatalogSkuItem(
            UUID id,
            String skuCode,
            Map<String, Object> attributes,
            int stockTotal,
            int stockAvailable
    ) {}
}
