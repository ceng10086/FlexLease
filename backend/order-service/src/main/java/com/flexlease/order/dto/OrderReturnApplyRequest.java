package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * OrderReturnApplyRequest 请求 DTO。
 */
public record OrderReturnApplyRequest(
        @NotNull UUID userId,
        String reason,
        String logisticsCompany,
        String trackingNumber
) {
}
