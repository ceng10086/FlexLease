package com.flexlease.order.dto;

import com.flexlease.order.domain.ExtensionRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * OrderExtensionResponse 响应 DTO。
 */
public record OrderExtensionResponse(
        UUID id,
        ExtensionRequestStatus status,
        int additionalMonths,
        UUID requestedBy,
        OffsetDateTime requestedAt,
        UUID decisionBy,
        OffsetDateTime decisionAt,
        String remark
) {
}
