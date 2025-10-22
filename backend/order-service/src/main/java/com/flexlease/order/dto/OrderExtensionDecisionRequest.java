package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderExtensionDecisionRequest(
        @NotNull UUID vendorId,
        boolean approve,
        String remark
) {
}
