package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderReturnApplyRequest(
        @NotNull UUID userId,
        String reason,
        String logisticsCompany,
        String trackingNumber
) {
}
