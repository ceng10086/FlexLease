package com.flexlease.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 通知模板响应 DTO。
 */
public record NotificationTemplateResponse(
        UUID id,
        String code,
        String subject,
        String content,
        OffsetDateTime createdAt
) {
}
