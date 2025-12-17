package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderInspectionRequest(
        @NotNull(message = "缺少厂商标识")
        UUID vendorId,
        String remark
) {
}

