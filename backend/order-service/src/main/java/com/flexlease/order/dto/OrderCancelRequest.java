package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderCancelRequest(
        @NotNull UUID userId,
        String reason
) {
}
