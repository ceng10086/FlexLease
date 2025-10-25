package com.flexlease.common.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record NotificationSendRequest(
        String templateCode,
        NotificationChannel channel,
        @NotBlank @Size(max = 100) String recipient,
        @Size(max = 200) String subject,
        String content,
        Map<String, Object> variables
) {
    public boolean hasTemplate() {
        return templateCode != null && !templateCode.isBlank();
    }
}
