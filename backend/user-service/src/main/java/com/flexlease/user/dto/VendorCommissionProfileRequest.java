package com.flexlease.user.dto;

import com.flexlease.common.user.CreditTier;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record VendorCommissionProfileRequest(
        @NotBlank(message = "行业分类不能为空")
        @Size(max = 100, message = "行业分类长度不能超过 100 字符")
        String industryCategory,
        @NotNull(message = "基础抽成比例不能为空")
        @DecimalMin(value = "0.00", message = "基础抽成比例不能为负")
        @DecimalMax(value = "0.30", message = "基础抽成比例不能超过 30%")
        BigDecimal baseRate,
        @NotNull(message = "信用档位不能为空")
        CreditTier creditTier,
        @NotNull(message = "SLA 评分不能为空")
        @Min(value = 0, message = "SLA 评分不能小于 0")
        @Max(value = 100, message = "SLA 评分不能超过 100")
        Integer slaScore
) {
}
