package com.flexlease.product.dto;

import com.flexlease.product.domain.RentalPlanType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 租赁方案创建/更新请求。
 */
public record RentalPlanRequest(
        @NotNull(message = "planType 不能为空")
        RentalPlanType planType,

        @Positive(message = "termMonths 必须大于 0")
        int termMonths,

        @NotNull(message = "depositAmount 不能为空")
        BigDecimal depositAmount,

        @NotNull(message = "rentAmountMonthly 不能为空")
        BigDecimal rentAmountMonthly,

        BigDecimal buyoutPrice,

        boolean allowExtend,

        String extensionUnit,

        BigDecimal extensionPrice
) {
}
