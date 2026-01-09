package com.flexlease.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 管理员人工调整信用分请求。
 */
public record CreditAdjustmentRequest(
        @NotNull(message = "调整值不能为空")
        Integer delta,
        String reason
) {
}
