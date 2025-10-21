package com.flexlease.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record CatalogProductResponse(
        ProductSummaryResponse product,
        List<RentalPlanCatalogItem> plans
) {

    public record RentalPlanCatalogItem(
            RentalPlanResponse plan,
            List<CatalogSkuItem> skus
    ) {}

    public record CatalogSkuItem(
            String skuCode,
            int stockAvailable,
            BigDecimal rentAmountMonthly,
            BigDecimal depositAmount
    ) {}
}
