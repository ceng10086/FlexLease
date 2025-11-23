package com.flexlease.order.dto;

import com.flexlease.order.domain.DisputeResolutionOption;
import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderDisputeStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderDisputeResponse(
        UUID id,
        OrderDisputeStatus status,
        UUID initiatorId,
        OrderActorRole initiatorRole,
        DisputeResolutionOption initiatorOption,
        String initiatorReason,
        String initiatorRemark,
        UUID respondentId,
        OrderActorRole respondentRole,
        DisputeResolutionOption respondentOption,
        String respondentRemark,
        OffsetDateTime respondedAt,
        OffsetDateTime deadlineAt,
        UUID escalatedBy,
        OffsetDateTime escalatedAt,
        DisputeResolutionOption adminDecisionOption,
        String adminDecisionRemark,
        UUID adminDecisionBy,
        OffsetDateTime adminDecisionAt,
        Integer userCreditDelta,
        int appealCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
