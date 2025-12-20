package com.flexlease.common.messaging;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * order-service 发布的订单领域事件消息体，
 * 供其他服务异步订阅与消费。
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
