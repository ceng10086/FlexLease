package com.flexlease.product.dto;

import com.flexlease.product.domain.ProductStatus;
import com.flexlease.product.domain.RentalPlanType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public record MediaAssetItem(
            UUID id,
            String fileUrl,
            Integer sortOrder
    ) {}

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

    public record CatalogSkuItem(
            UUID id,
            String skuCode,
            Map<String, Object> attributes,
            int stockTotal,
            int stockAvailable
    ) {}
}
