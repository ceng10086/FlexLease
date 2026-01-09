package com.flexlease.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 信用分人工调整记录响应。
 */
public record CreditAdjustmentResponse(
        UUID id,
        UUID userId,
        int delta,
        String reason,
        UUID operatorId,
        OffsetDateTime createdAt
) {
}
