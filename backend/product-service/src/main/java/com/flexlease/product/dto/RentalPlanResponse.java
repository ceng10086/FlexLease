package com.flexlease.product.dto;

import com.flexlease.product.domain.RentalPlanStatus;
import com.flexlease.product.domain.RentalPlanType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 租赁方案详情响应（含 SKU 列表）。
 */
public record RentalPlanResponse(
        UUID id,
        RentalPlanType planType,
        int termMonths,
        BigDecimal depositAmount,
        BigDecimal rentAmountMonthly,
        BigDecimal buyoutPrice,
        boolean allowExtend,
        String extensionUnit,
        BigDecimal extensionPrice,
        RentalPlanStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<SkuResponse> skus
) {
}
