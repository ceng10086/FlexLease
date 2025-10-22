package com.flexlease.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderExtensionApplyRequest(
        @NotNull UUID userId,
        @Min(1) int additionalMonths,
        String remark
) {
}
