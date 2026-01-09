package com.flexlease.order.dto;

import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderProofType;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * OrderProofResponse 响应 DTO。
 */
public record OrderProofResponse(
        UUID id,
        OrderProofType proofType,
        String description,
        String fileUrl,
        String contentType,
        long fileSize,
        UUID uploadedBy,
        OrderActorRole actorRole,
        OffsetDateTime uploadedAt
) {
}
