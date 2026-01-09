package com.flexlease.notification.dto;

import com.flexlease.notification.domain.NotificationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 通知日志响应 DTO（站内信）。
 */
public record NotificationLogResponse(
        UUID id,
        String templateCode,
        String recipient,
        String subject,
        String content,
        NotificationStatus status,
        String errorMessage,
        String contextType,
        String contextReference,
        OffsetDateTime sentAt,
        OffsetDateTime createdAt
) {
}
