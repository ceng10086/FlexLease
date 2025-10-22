package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderReturnDecisionRequest(
        @NotNull UUID vendorId,
        boolean approve,
        String remark
) {
}
