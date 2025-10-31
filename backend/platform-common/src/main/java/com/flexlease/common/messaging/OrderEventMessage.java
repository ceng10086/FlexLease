package com.flexlease.common.messaging;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Payload published by order-service to describe domain events that should be
 * consumed by other services asynchronously.
 */
public record OrderEventMessage(
        UUID orderId,
        String orderNo,
        UUID userId,
        UUID vendorId,
        String status,
        String eventType,
        OffsetDateTime occurredAt,
        UUID actorId,
        String description,
        Map<String, Object> attributes
) {
}
