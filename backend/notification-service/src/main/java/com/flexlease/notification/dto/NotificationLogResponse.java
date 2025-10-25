package com.flexlease.notification.dto;

import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.notification.domain.NotificationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationLogResponse(
        UUID id,
        String templateCode,
        NotificationChannel channel,
        String recipient,
        String subject,
        String content,
        NotificationStatus status,
        String errorMessage,
        OffsetDateTime sentAt,
        OffsetDateTime createdAt
) {
}
