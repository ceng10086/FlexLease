package com.flexlease.order.dto;

import com.flexlease.order.domain.ReturnRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderReturnResponse(
        UUID id,
        ReturnRequestStatus status,
        String reason,
        String logisticsCompany,
        String trackingNumber,
        UUID requestedBy,
        OffsetDateTime requestedAt,
        UUID decisionBy,
        OffsetDateTime decisionAt,
        String remark
) {
}
