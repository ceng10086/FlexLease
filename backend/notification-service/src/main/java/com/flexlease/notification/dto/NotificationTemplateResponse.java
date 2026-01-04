package com.flexlease.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationTemplateResponse(
        UUID id,
        String code,
        String subject,
        String content,
        OffsetDateTime createdAt
) {
}
