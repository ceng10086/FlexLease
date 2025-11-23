package com.flexlease.user.dto;

import com.flexlease.common.user.CreditTier;
import java.math.BigDecimal;

public record VendorCommissionProfileResponse(
        String industryCategory,
        BigDecimal baseRate,
        CreditTier creditTier,
        Integer slaScore,
        BigDecimal commissionRate
) {
}
