package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * OrderBuyoutDecisionRequest 请求 DTO。
 */
public record OrderBuyoutDecisionRequest(
        @NotNull UUID vendorId,
        boolean approve,
        String remark
) {
}
