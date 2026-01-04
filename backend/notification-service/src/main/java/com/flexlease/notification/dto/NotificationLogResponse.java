package com.flexlease.notification.dto;

import com.flexlease.notification.domain.NotificationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

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
