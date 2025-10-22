package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderBuyoutApplyRequest(
        @NotNull UUID userId,
        BigDecimal buyoutAmount,
        String remark
) {
}
