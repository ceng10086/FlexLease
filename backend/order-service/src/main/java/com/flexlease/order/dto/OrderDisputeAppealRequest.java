package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderDisputeAppealRequest(
        @NotNull(message = "缺少操作人")
        UUID actorId,
        String reason
) {
}
