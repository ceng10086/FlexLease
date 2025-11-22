package com.flexlease.user.dto;

import jakarta.validation.constraints.NotNull;

public record CreditAdjustmentRequest(
        @NotNull(message = "调整值不能为空")
        Integer delta,
        String reason
) {
}
