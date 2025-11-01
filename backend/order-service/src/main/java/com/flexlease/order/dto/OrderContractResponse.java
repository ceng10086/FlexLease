package com.flexlease.order.dto;

import com.flexlease.order.domain.OrderContractStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderContractResponse(
        UUID contractId,
        UUID orderId,
        String contractNumber,
        OrderContractStatus status,
        String content,
        String signature,
        UUID signedBy,
        OffsetDateTime generatedAt,
        OffsetDateTime signedAt,
        OffsetDateTime updatedAt
) {
}
