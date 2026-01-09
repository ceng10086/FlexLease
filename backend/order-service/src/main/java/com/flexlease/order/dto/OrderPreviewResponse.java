package com.flexlease.order.dto;

import com.flexlease.common.user.CreditTier;
import java.math.BigDecimal;

/**
 * OrderPreviewResponse 响应 DTO。
 */
public record OrderPreviewResponse(
        BigDecimal depositAmount,
        BigDecimal rentAmount,
        BigDecimal totalAmount,
        BigDecimal originalDepositAmount,
        CreditSnapshot creditSnapshot
) {

    public record CreditSnapshot(
            Integer creditScore,
            CreditTier creditTier,
            BigDecimal depositAdjustmentRate,
            boolean requiresManualReview
    ) {
    }
}
