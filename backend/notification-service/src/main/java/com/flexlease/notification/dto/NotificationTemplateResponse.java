package com.flexlease.notification.dto;

import com.flexlease.common.notification.NotificationChannel;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationTemplateResponse(
        UUID id,
        String code,
        NotificationChannel channel,
        String subject,
        String content,
        OffsetDateTime createdAt
) {
}
