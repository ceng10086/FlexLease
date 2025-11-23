package com.flexlease.order.dto;

import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderEventType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderEventResponse(
        UUID id,
        OrderEventType eventType,
        String description,
        UUID createdBy,
        OrderActorRole actorRole,
        OffsetDateTime createdAt
) {
}
