package com.flexlease.order.dto;

import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderSurveyStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * OrderSurveyResponse 响应 DTO。
 */
public record OrderSurveyResponse(
        UUID id,
        UUID disputeId,
        OrderSurveyStatus status,
        OrderActorRole targetRole,
        UUID targetRef,
        Integer rating,
        String comment,
        OffsetDateTime requestedAt,
        OffsetDateTime availableAt,
        OffsetDateTime submittedAt
) {
}
