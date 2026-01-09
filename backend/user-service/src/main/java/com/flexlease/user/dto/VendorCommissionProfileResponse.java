package com.flexlease.user.dto;

import com.flexlease.common.user.CreditTier;
import java.math.BigDecimal;

/**
 * 厂商抽成画像响应（含计算后的实际抽成比例 {@code commissionRate}）。
 */
public record VendorCommissionProfileResponse(
        String industryCategory,
        BigDecimal baseRate,
        CreditTier creditTier,
        Integer slaScore,
        BigDecimal commissionRate
) {
}
