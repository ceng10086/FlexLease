package com.flexlease.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreditAdjustmentResponse(
        UUID id,
        UUID userId,
        int delta,
        String reason,
        UUID operatorId,
        OffsetDateTime createdAt
) {
}
